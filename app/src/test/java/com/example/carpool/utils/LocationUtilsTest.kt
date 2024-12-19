
package com.example.carpool.utils

import org.junit.Assert.*
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testCalculateDistance() {
        // Coordinates for New York City and Los Angeles
        val nycLat = 40.7128
        val nycLon = -74.0060
        val laLat = 34.0522
        val laLon = -118.2437

        val distance = calculateDistance(nycLat, nycLon, laLat, laLon)
        // The approximate distance is 3936 km
        assertTrue(distance > 3900 && distance < 4000)
    }
}