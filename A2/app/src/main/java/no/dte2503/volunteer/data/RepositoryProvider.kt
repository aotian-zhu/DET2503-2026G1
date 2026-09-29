package no.dte2503.volunteer.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import no.dte2503.volunteer.BuildConfig

object RepositoryProvider {
    val repository: VolunteerRepository by lazy {
        val url = BuildConfig.SUPABASE_URL.trim()
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (url.isBlank() || anonKey.isBlank()) {
            MockVolunteerRepository()
        } else {
            val client = createSupabaseClient(url, anonKey) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
            SupabaseVolunteerRepository(client)
        }
    }
}
