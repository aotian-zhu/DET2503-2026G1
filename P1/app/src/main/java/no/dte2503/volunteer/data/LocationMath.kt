package no.dte2503.volunteer.data

import java.util.Locale
import kotlin.math.*

// Pure geometry still works without network access or map imagery.
fun distanceMetres(from: GeoPointData, to: GeoPointData): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = (sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)).coerceIn(0.0, 1.0)
    return (6_371_000.0 * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
}

fun tasksByDistance(tasks: List<VolunteerTask>, position: GeoPointData?) = tasks.map {
    TaskWithDistance(it, position?.let { current -> distanceMetres(current, it.place.position) })
}.sortedBy { it.distanceMetres ?: Float.MAX_VALUE }

fun formatDistance(metres: Float): String = if (metres < 1000) "${metres.roundToInt()} m" else String.format(Locale.UK, "%.1f km", metres / 1000)

fun validateCheckIn(payload: String, shiftId: String) {
    val parts = payload.trim().split(':')
    require(parts.size == 5 && parts[0] == "arena-checkin" && parts[1] == "v1") { "This is not an Arena Companion checkpoint code." }
    require(parts[2] == shiftId) { "This checkpoint belongs to another shift." }
    require(runCatching { java.util.UUID.fromString(parts[3]) }.isSuccess && parts[4].isNotBlank()) { "The checkpoint code is incomplete." }
}
