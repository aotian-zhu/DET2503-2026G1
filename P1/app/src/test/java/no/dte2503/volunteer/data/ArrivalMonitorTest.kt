package no.dte2503.volunteer.data

import org.junit.Assert.*
import org.junit.Test

class ArrivalMonitorTest {
    private val hub = MockRepository.centre
    private fun north(metres: Double) = GeoPointData(hub.latitude + metres / 111_195, hub.longitude)

    @Test fun entryOnlyAlertsOnceUntilAConfirmedExit() {
        val monitor = ArrivalMonitor()
        assertEquals(ArrivalState.OUTSIDE, monitor.evaluate("hub", hub, north(400.0), 5f).state)
        assertTrue(monitor.evaluate("hub", hub, hub, 5f).entered)
        assertFalse(monitor.evaluate("hub", hub, hub, 5f).entered)
        assertEquals(ArrivalState.UNCERTAIN, monitor.evaluate("hub", hub, north(160.0), 15f).state)
        assertFalse(monitor.evaluate("hub", hub, hub, 5f).entered)
        assertEquals(ArrivalState.OUTSIDE, monitor.evaluate("hub", hub, north(300.0), 5f).state)
        assertTrue(monitor.evaluate("hub", hub, hub, 5f).entered)
    }

    @Test fun coarseAndUncertainLocationsCannotClaimArrival() {
        val monitor = ArrivalMonitor()
        assertEquals(ArrivalState.UNCERTAIN, monitor.evaluate("hub", hub, hub, 2000f).state)
        assertFalse(monitor.evaluate("hub", hub, north(145.0), 20f).entered)
        assertTrue(monitor.evaluate("hub", hub, hub, 5f).entered)
    }

    @Test fun missingOrInvalidAccuracyCannotInventAnArrival() {
        val monitor = ArrivalMonitor()
        assertEquals(ArrivalState.WAITING, monitor.evaluate("hub", hub, null, null).state)
        listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            val result = monitor.evaluate("hub", hub, hub, it)
            assertEquals(ArrivalState.UNCERTAIN, result.state)
            assertFalse(result.entered)
        }
        assertEquals(ArrivalState.OFF, monitor.evaluate(null, null, hub, 5f).state)
    }

    @Test fun switchingAccountOrPreviewResetsTheDetector() {
        val monitor = ArrivalMonitor()
        assertTrue(monitor.evaluate("user-one:real:hub", hub, hub, 5f).entered)
        assertFalse(monitor.evaluate("user-one:real:hub", hub, hub, 5f).entered)
        assertTrue(monitor.evaluate("user-two:real:hub", hub, hub, 5f).entered)
        assertTrue(monitor.evaluate("user-two:practice:hub", hub, hub, 5f).entered)
    }

    @Test fun temporaryMissingPositionDoesNotRepeatAnInsideAlert() {
        val monitor = ArrivalMonitor()
        assertTrue(monitor.evaluate("hub", hub, hub, 5f).entered)
        assertEquals(ArrivalState.WAITING, monitor.evaluate("hub", hub, null, null).state)
        assertFalse(monitor.evaluate("hub", hub, hub, 5f).entered)
    }

    @Test fun walkingTimeHasBoundsAndHonestPrecisionHandling() {
        assertEquals(5, estimatedWalkingMinutes(300f))
        assertEquals(1, estimatedWalkingMinutes(0f))
        assertNull(estimatedWalkingMinutes(-1f))
        assertNull(estimatedWalkingMinutes(20_001f))
        assertNull(estimatedWalkingMinutes(Float.NaN))
        assertEquals("About 5 min on foot", walkingEstimate(300f, 5f))
        assertTrue(walkingEstimate(300f, 2000f).contains("precise"))
        assertTrue(walkingEstimate(300f, null).contains("precise"))
        assertEquals("Outside walking range", walkingEstimate(50_000f, 5f))
    }
}
