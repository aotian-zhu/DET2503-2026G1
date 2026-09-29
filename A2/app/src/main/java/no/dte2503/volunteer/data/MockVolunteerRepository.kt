package no.dte2503.volunteer.data

import java.time.Instant
import java.util.UUID

class MockVolunteerRepository : VolunteerRepository {
    override val isRemote = false
    private var announcements = MockRepository.announcements
    private var conversations = MockRepository.conversations
    private val checkIns = mutableMapOf<String, CheckInResult>()

    override suspend fun restoreSession() = false
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun loadVolunteerData() = VolunteerData(
        MockRepository.profile,
        MockRepository.shift,
        MockRepository.locations,
        MockRepository.tasks,
        announcements,
        conversations,
    )
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) = Unit

    override suspend fun markAnnouncementRead(announcementId: String) {
        announcements = announcements.map { if (it.id == announcementId) it.copy(isRead = true) else it }
    }

    override suspend fun sendMessage(taskId: String, body: String): TaskConversation {
        val message = ChatMessage("message_${UUID.randomUUID()}", "Volunteer", body, "Now", true)
        val existing = conversations.firstOrNull { it.taskId == taskId }
        val updated = existing?.copy(messages = existing.messages + message, unreadCount = 0)
            ?: TaskConversation("conversation_$taskId", taskId, "Shift coordinator", listOf(message), 0)
        conversations = conversations.filterNot { it.taskId == taskId } + updated
        return updated
    }

    override suspend fun checkIn(payload: String): CheckInResult {
        require(payload.startsWith("arena-checkin:v1:")) { "Invalid check-in QR code" }
        return checkIns[payload]?.copy(duplicate = true) ?: CheckInResult(
            id = "checkin_${UUID.randomUUID()}",
            checkedInAt = Instant.now().toString(),
            duplicate = false,
        ).also { checkIns[payload] = it }
    }

    override suspend fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?) = IncidentReceipt(
        id = "incident_${UUID.randomUUID()}",
        submittedAt = Instant.now().toString(),
        photoPath = photoBytes?.let { "demo/incidents/photo.jpg" },
    )
}
