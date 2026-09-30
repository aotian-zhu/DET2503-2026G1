package no.dte2503.volunteer.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseVolunteerRepository(private val client: SupabaseClient) : VolunteerRepository {
    override val isRemote = true
    override suspend fun restoreSession() = client.auth.currentSessionOrNull() != null
    override suspend fun signIn(email: String, password: String) { client.auth.signInWith(Email) { this.email = email; this.password = password } }
    override suspend fun signOut() = client.auth.signOut()

    override suspend fun loadVolunteerData(): VolunteerData {
        // 将多个受 RLS 保护的表聚合成一次 UI 所需快照，关联关系在仓库层完成。
        val user = requireNotNull(client.auth.currentUserOrNull()) { "No authenticated user" }
        val profile = client.from("profiles").select { filter { eq("id", user.id) } }.decodeSingle<ProfileDto>().toModel(user.email.orEmpty())
        val shifts = client.from("shifts").select { filter { eq("volunteer_id", user.id) } }.decodeList<ShiftDto>()
        val locations = client.from("locations").select().decodeList<LocationDto>().map(LocationDto::toModel)
        val tasks = client.from("tasks").select { filter { eq("volunteer_id", user.id) } }.decodeList<TaskDto>().map(TaskDto::toModel)
        val reads = client.from("announcement_reads").select { filter { eq("volunteer_id", user.id) } }.decodeList<AnnouncementReadDto>().map { it.announcementId }.toSet()
        val announcements = client.from("announcements").select().decodeList<AnnouncementDto>().map { it.toModel(it.id in reads) }
        val conversationDtos = client.from("conversations").select().decodeList<ConversationDto>()
        val taskIds = tasks.map { it.id }.toSet()
        val conversations = conversationDtos.filter { it.taskId in taskIds }.map { conversation ->
            val messages = client.from("messages").select { filter { eq("conversation_id", conversation.id) } }.decodeList<MessageDto>()
            conversation.toModel(messages)
        }
        return VolunteerData(profile, shifts.firstOrNull()?.toModel() ?: Shift("", "No active shift", "", "", ""), locations, tasks, announcements, conversations)
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        // 写操作通过受控 RPC 收口，数据库可同时校验身份、任务归属与输入范围。
        client.postgrest.rpc("update_own_task_status", buildJsonObject { put("task_id", taskId); put("new_status", status.name) })
    }

    override suspend fun markAnnouncementRead(announcementId: String) {
        client.postgrest.rpc("mark_announcement_read", buildJsonObject { put("announcement_id", announcementId) })
    }

    override suspend fun sendMessage(taskId: String, body: String): TaskConversation {
        val conversationId = client.postgrest.rpc("send_task_message", buildJsonObject { put("task_id", taskId); put("message_body", body) }).decodeAs<String>()
        val conversation = client.from("conversations").select { filter { eq("id", conversationId) } }.decodeSingle<ConversationDto>()
        val messages = client.from("messages").select { filter { eq("conversation_id", conversationId) } }.decodeList<MessageDto>()
        return conversation.toModel(messages)
    }

    override suspend fun checkIn(payload: String): CheckInResult = client.postgrest.rpc(
        "check_in_with_qr", buildJsonObject { put("qr_payload", payload) },
    ).decodeSingle<CheckInDto>().toModel()

    override suspend fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?): IncidentReceipt {
        val userId = requireNotNull(client.auth.currentUserOrNull()).id
        val photoPath = photoBytes?.let {
            val extension = when (photoMimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            "$userId/${UUID.randomUUID()}.$extension".also { path -> client.storage.from("incident-photos").upload(path, it) { upsert = false } }
        }
        return runCatching {
            client.postgrest.rpc("submit_incident", buildJsonObject {
                put("incident_category", draft.category.name)
                put("incident_description", draft.description)
                draft.locationId?.let { put("incident_location_id", it) }
                draft.taskId?.let { put("incident_task_id", it) }
                photoPath?.let { put("incident_photo_path", it) }
            }).decodeSingle<IncidentDto>().toModel()
        }.getOrElse { error ->
            // 照片先上传、事件后落库；RPC 失败时删除孤立对象，补偿非事务的跨服务操作。
            photoPath?.let { runCatching { client.storage.from("incident-photos").delete(it) } }
            throw error
        }
    }
}

@Serializable private data class ProfileDto(val id: String, @SerialName("display_name") val displayName: String, val phone: String = "", @SerialName("assigned_function_ids") val assignedFunctionIds: List<String> = emptyList()) { fun toModel(email: String) = VolunteerProfile(id, displayName, email, phone, assignedFunctionIds.toSet()) }
@Serializable private data class ShiftDto(val id: String, val title: String, @SerialName("start_time") val startTime: String, @SerialName("end_time") val endTime: String, @SerialName("meeting_point_id") val meetingPointId: String) { fun toModel() = Shift(id, title, startTime, endTime, meetingPointId) }
@Serializable private data class LocationDto(val id: String, val name: String, val description: String, val latitude: Double, val longitude: Double, @SerialName("scenario_ids") val scenarioIds: List<String> = emptyList()) { fun toModel() = ServiceLocation(id, name, description, GeoPointData(latitude, longitude), scenarioIds.toSet()) }
@Serializable private data class TaskDto(val id: String, val title: String, val details: String, @SerialName("location_id") val locationId: String, val latitude: Double, val longitude: Double, val area: String, val floor: String? = null, @SerialName("scenario_id") val scenarioId: String, @SerialName("required_permission_ids") val requiredPermissionIds: List<String> = emptyList(), @SerialName("due_time") val dueTime: String, val status: TaskStatus) { fun toModel() = VolunteerTask(id, title, details, locationId, TaskPlace(GeoPointData(latitude, longitude), area, floor), scenarioId, requiredPermissionIds.toSet(), dueTime, status) }
@Serializable private data class AnnouncementReadDto(@SerialName("announcement_id") val announcementId: String)
@Serializable private data class AnnouncementDto(val id: String, val title: String, val body: String, val sender: String, @SerialName("sent_at") val sentAt: String, val priority: MessagePriority) { fun toModel(read: Boolean) = Announcement(id, title, body, sender, sentAt.formatTime(), priority, read) }
@Serializable private data class ConversationDto(val id: String, @SerialName("task_id") val taskId: String, @SerialName("coordinator_name") val coordinatorName: String) { fun toModel(messages: List<MessageDto>) = TaskConversation(id, taskId, coordinatorName, messages.map(MessageDto::toModel), 0) }
@Serializable private data class MessageDto(val id: String, val sender: String, val body: String, @SerialName("sent_at") val sentAt: String, @SerialName("is_from_volunteer") val isFromVolunteer: Boolean) { fun toModel() = ChatMessage(id, sender, body, sentAt.formatTime(), isFromVolunteer) }
@Serializable private data class CheckInDto(val id: String, @SerialName("checked_in_at") val checkedInAt: String, val duplicate: Boolean) { fun toModel() = CheckInResult(id, checkedInAt, duplicate) }
@Serializable private data class IncidentDto(val id: String, @SerialName("submitted_at") val submittedAt: String, @SerialName("photo_path") val photoPath: String? = null) { fun toModel() = IncidentReceipt(id, submittedAt, photoPath) }
private fun String.formatTime() = substringAfter('T', this).take(5)
