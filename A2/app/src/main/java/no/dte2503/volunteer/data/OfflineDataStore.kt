package no.dte2503.volunteer.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OfflineDataStore(context: Context) {
    private val preferences = context.getSharedPreferences("offline_data", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(userId: String, data: VolunteerData) {
        preferences.edit().putString(snapshotKey(userId), json.encodeToString(data)).putString(LAST_USER_KEY, userId).apply()
    }

    fun load(userId: String): VolunteerData? = preferences.getString(snapshotKey(userId), null)?.let { runCatching { json.decodeFromString<VolunteerData>(it) }.getOrNull() }

    fun lastUserId(): String? = preferences.getString(LAST_USER_KEY, null)

    fun clear(userId: String) = preferences.edit().remove(snapshotKey(userId)).remove(LAST_USER_KEY).apply()

    private fun snapshotKey(userId: String) = "snapshot_$userId"
    private companion object { const val LAST_USER_KEY = "last_user_id" }
}

@Serializable
data class PendingOperation(
    val id: String,
    val userId: String,
    val type: PendingOperationType,
    val taskId: String? = null,
    val taskStatus: TaskStatus? = null,
    val announcementId: String? = null,
    val body: String? = null,
    val qrPayload: String? = null,
    val incident: IncidentDraft? = null,
    val photoPath: String? = null,
    val photoMimeType: String? = null,
)

@Serializable
enum class PendingOperationType { UPDATE_TASK, MARK_ANNOUNCEMENT_READ, SEND_MESSAGE, CHECK_IN, SUBMIT_INCIDENT }

class OfflineOperationStore(context: Context) {
    private val preferences = context.getSharedPreferences("offline_operations", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun add(operation: PendingOperation) {
        val operations = all().filterNot { it.id == operation.id } + operation
        preferences.edit().putString(KEY, json.encodeToString(operations)).apply()
    }

    fun all(): List<PendingOperation> = preferences.getString(KEY, null)?.let { runCatching { json.decodeFromString<List<PendingOperation>>(it) }.getOrNull() } ?: emptyList()

    fun remove(id: String) = preferences.edit().putString(KEY, json.encodeToString(all().filterNot { it.id == id })).apply()

    private companion object { const val KEY = "operations" }
}
