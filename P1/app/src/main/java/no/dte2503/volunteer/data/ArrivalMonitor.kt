package no.dte2503.volunteer.data

import kotlin.math.ceil

const val ARRIVAL_RADIUS_METRES = 150f
private const val EXIT_RADIUS_METRES = 200f

enum class ArrivalState { OFF, WAITING, OUTSIDE, UNCERTAIN, INSIDE }
data class ArrivalResult(val state: ArrivalState = ArrivalState.OFF, val entered: Boolean = false, val distanceMetres: Float? = null)

// Foreground geofencing with a wider exit boundary to avoid repeated GPS-jitter alerts.
// An imprecise fix cannot confirm entry. No location is saved or sent to the backend.
class ArrivalMonitor {
    private var ownerAndTarget: String? = null
    private var wasInside = false

    fun evaluate(key: String?, target: GeoPointData?, position: GeoPointData?, accuracy: Float?): ArrivalResult {
        if (key != ownerAndTarget) { ownerAndTarget = key; wasInside = false }
        if (key == null || target == null) return ArrivalResult()
        if (position == null || accuracy == null) return ArrivalResult(ArrivalState.WAITING)
        val distance = distanceMetres(position, target)
        if (!accuracy.isFinite() || accuracy < 0 || accuracy > 100 || !distance.isFinite()) return ArrivalResult(ArrivalState.UNCERTAIN, distanceMetres = distance)
        if (distance + accuracy <= ARRIVAL_RADIUS_METRES) {
            val entered = !wasInside
            wasInside = true
            return ArrivalResult(ArrivalState.INSIDE, entered, distance)
        }
        if (distance - accuracy >= EXIT_RADIUS_METRES) {
            wasInside = false
            return ArrivalResult(ArrivalState.OUTSIDE, distanceMetres = distance)
        }
        return ArrivalResult(ArrivalState.UNCERTAIN, distanceMetres = distance)
    }
}

// Walking estimate from straight-line distance at 4 km/h, not route guidance.
fun estimatedWalkingMinutes(metres: Float): Int? = if (metres.isFinite() && metres in 0f..20_000f) ceil(metres / (4_000f / 60f)).toInt().coerceAtLeast(1) else null
fun walkingEstimate(metres: Float, accuracy: Float?): String = when {
    accuracy == null || !accuracy.isFinite() || accuracy > 100 -> "A more precise fix is needed for a walking estimate"
    else -> estimatedWalkingMinutes(metres)?.let { "About $it min on foot" } ?: "Outside walking range"
}

data class DemoWalkPoint(val id: String, val label: String, val position: GeoPointData)
val demoWalkPoints = listOf(
    DemoWalkPoint("station", "Station", GeoPointData(68.4417, 17.4411)),
    DemoWalkPoint("outside", "Outside hub", GeoPointData(68.4420, 17.4272)),
    DemoWalkPoint("inside", "Inside hub", MockRepository.centre),
)
