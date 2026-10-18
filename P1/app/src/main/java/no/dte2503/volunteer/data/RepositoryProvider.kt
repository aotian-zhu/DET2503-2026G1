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
        val isolatedTestFile = if (BuildConfig.DEBUG) testStateFile else null
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        val remote = if (isolatedTestFile != null || url.isBlank() || key.isBlank()) null else {
            val client = createSupabaseClient(url, key) { install(Auth); install(Postgrest); install(Storage) }
            SupabaseVolunteerRepository(client)
        }
        return AppDependencies(
            ArenaRepository(
                AtomicLocalStore(isolatedTestFile ?: File(context.filesDir, "arena-state.json")), network.online,
                // UI tests verify local workflows without repeatedly creating real auth/HTTP clients.
                // The normal installed app uses the real services; storage/sync have separate device tests.
                if (isolatedTestFile != null) WeatherSource { WeatherSnapshot(2.0, -1.0, 12.0, 3, "2026-10-18T10:00", System.currentTimeMillis()) } else OpenMeteoWeatherSource(),
                remote, LocalPhotoStore(File(context.filesDir, "incident-photos")),
            ), network,
        )
    }
}
