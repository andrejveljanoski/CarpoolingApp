// app/src/main/java/com/example/carpool/FetchDrivers.kt
package com.example.carpool


import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun fetchDrivers(
    apiKey: String,
    source: LatLng,
    destination: LatLng
): Pair<List<Map<String, Any>>, String> = suspendCoroutine { continuation ->
    fetchAvailableDrivers(apiKey, source, destination) { drivers, travelTime ->
        continuation.resume(Pair(drivers, travelTime) as Pair<List<Map<String, Any>>, String>)
    }
}