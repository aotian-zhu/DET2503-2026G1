package no.dte2503.volunteer.data

// Network boundary inherited from A2. ArenaRepository adds local persistence and sync.
interface VolunteerRepository {
    val isRemote: Boolean
    val signedInUserId: String?
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

