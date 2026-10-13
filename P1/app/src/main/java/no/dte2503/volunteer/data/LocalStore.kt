package no.dte2503.volunteer.data

import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LocalStore {
    val state: StateFlow<LocalState>
    suspend fun initialize()
    suspend fun update(change: (LocalState) -> LocalState)
}

// AtomicFile retains the old snapshot if a write is interrupted. Publish after disk commit.
class AtomicLocalStore(private val file: File) : LocalStore {
    private val atomicFile = AtomicFile(file)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutable = MutableStateFlow(LocalState())
    override val state = mutable.asStateFlow()

    override suspend fun initialize() = mutex.withLock {
        val loaded = withContext(Dispatchers.IO) {
            if (!file.exists() && !File(file.path + ".bak").exists()) LocalState()
            else json.decodeFromString<LocalState>(atomicFile.openRead().bufferedReader().use { it.readText() })
        }
        require(loaded.schemaVersion == 1) { "This saved data uses a newer app version." }
        mutable.value = loaded
    }

    override suspend fun update(change: (LocalState) -> LocalState) = mutex.withLock {
        val next = change(mutable.value)
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val output = atomicFile.startWrite()
            try {
                output.write(json.encodeToString(next).toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(output)
            } catch (error: Exception) {
                atomicFile.failWrite(output)
                throw error
            }
        }
        mutable.value = next
    }
}

interface PhotoStore {
    suspend fun save(id: String, bytes: ByteArray, mimeType: String): String
    suspend fun delete(path: String)
}

class LocalPhotoStore(private val directory: File) : PhotoStore {
    override suspend fun save(id: String, bytes: ByteArray, mimeType: String): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val extension = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val target = File(directory, "$id.$extension")
        val atomic = AtomicFile(target)
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
        target.absolutePath
    }
    override suspend fun delete(path: String) { withContext(Dispatchers.IO) { File(path).delete() } }
}
