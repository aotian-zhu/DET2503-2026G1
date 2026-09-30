package no.dte2503.volunteer.data

// UI 层只依赖此边界；认证、网络、缓存或演示实现均封装在仓库内部。
interface VolunteerRepository {
    val isRemote: Boolean
    suspend fun restoreSession(): Boolean
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    suspend fun loadVolunteerData(): VolunteerData
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    suspend fun markAnnouncementRead(announcementId: String)
    suspend fun sendMessage(taskId: String, body: String): TaskConversation
    suspend fun checkIn(payload: String): CheckInResult
    suspend fun submitIncident(draft: IncidentDraft, photoBytes: ByteArray?, photoMimeType: String?): IncidentReceipt
}

data class VolunteerData(
    val profile: VolunteerProfile,
    val shift: Shift,
    val locations: List<ServiceLocation>,
    val tasks: List<VolunteerTask>,
    val announcements: List<Announcement>,
    val conversations: List<TaskConversation>,
)
