package no.dte2503.volunteer.data

import android.content.Context
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import java.io.File
import no.dte2503.volunteer.BuildConfig
import no.dte2503.volunteer.platform.NetworkMonitor

data class AppDependencies(val repository: ArenaRepository, val network: NetworkMonitor)

object RepositoryProvider {
    // Instrumented tests use an isolated file and never reset a volunteer's real data.
    @Volatile internal var testStateFile: File? = null
    fun create(context: Context): AppDependencies {
        val network = NetworkMonitor(context.applicationContext)
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        val remote = if (url.isBlank() || key.isBlank()) null else {
            val client = createSupabaseClient(url, key) { install(Auth); install(Postgrest); install(Storage) }
            SupabaseVolunteerRepository(client)
        }
        return AppDependencies(
            ArenaRepository(
                AtomicLocalStore(if (BuildConfig.DEBUG) testStateFile ?: File(context.filesDir, "arena-state.json") else File(context.filesDir, "arena-state.json")), network.online,
                OpenMeteoWeatherSource(), remote, LocalPhotoStore(File(context.filesDir, "incident-photos")),
            ), network,
        )
    }
}
