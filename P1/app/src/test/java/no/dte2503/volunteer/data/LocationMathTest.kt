package no.dte2503.volunteer.data

import org.junit.Assert.*
import org.junit.Test

class LocationMathTest {
    @Test fun samePositionHasZeroDistance() { assertEquals(0f, distanceMetres(MockRepository.centre, MockRepository.centre), .01f) }
    @Test fun narvikStationDistanceIsPlausibleAndSymmetric() {
        val station = GeoPointData(68.4417, 17.4411)
        val forward = distanceMetres(MockRepository.centre, station)
        assertTrue(forward in 600f..800f)
        assertEquals(forward, distanceMetres(station, MockRepository.centre), .01f)
    }
    @Test fun missingGpsDoesNotInventDistances() {
        val items = tasksByDistance(MockRepository.tasks, null)
        assertTrue(items.all { it.distanceMetres == null })
        assertEquals(MockRepository.tasks.map { it.id }, items.map { it.task.id })
    }
    @Test fun nearestUnfinishedTaskChangesWithPosition() {
        val open = MockRepository.tasks.filter { it.status != TaskStatus.DONE }
        assertEquals("task_02", tasksByDistance(open, GeoPointData(68.4415, 17.4410)).first().task.id)
        assertEquals("task_03", tasksByDistance(open, GeoPointData(68.4326, 17.4451)).first().task.id)
    }
    @Test fun distanceLabelsUseMetresAndKilometres() { assertEquals("850 m", formatDistance(850f)); assertEquals("1.2 km", formatDistance(1200f)) }
    @Test fun antipodalDistanceIsFinite() { assertTrue(distanceMetres(GeoPointData(0.0, 0.0), GeoPointData(0.0, 180.0)).isFinite()) }
}
