package no.dte2503.volunteer.data

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

fun interface WeatherSource { suspend fun fetch(): WeatherSnapshot }

// Only the fixed Narvik venue coordinate is sent. The volunteer's GPS fix stays on-device.
class OpenMeteoWeatherSource : WeatherSource {
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun fetch(): WeatherSnapshot = withContext(Dispatchers.IO) {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=68.4384&longitude=17.4272&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&timezone=Europe%2FOslo&forecast_days=1")
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "ArenaCompanion-P1/0.5 (Android course prototype)")
            require(connection.responseCode in 200..299) { "Weather service is temporarily unavailable." }
            val response = json.decodeFromString<WeatherResponse>(connection.inputStream.bufferedReader().use { it.readText() })
            val current = response.current
            require(current.temperature.isFinite() && current.feelsLike.isFinite() && current.wind.isFinite()) { "Weather service returned invalid readings." }
            WeatherSnapshot(current.temperature, current.feelsLike, current.wind, current.code, current.time, System.currentTimeMillis())
        } finally { connection.disconnect() }
    }
}

@Serializable private data class WeatherResponse(val current: CurrentWeather)
@Serializable private data class CurrentWeather(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val feelsLike: Double,
    @SerialName("wind_speed_10m") val wind: Double,
    @SerialName("weather_code") val code: Int,
)

fun weatherDescription(code: Int) = when (code) {
    0 -> "Clear skies"; 1, 2 -> "Partly cloudy"; 3 -> "Overcast"; 45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"; 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
    71, 73, 75, 77, 85, 86 -> "Snow"; 95, 96, 99 -> "Thunderstorms"; else -> "Current conditions"
}
