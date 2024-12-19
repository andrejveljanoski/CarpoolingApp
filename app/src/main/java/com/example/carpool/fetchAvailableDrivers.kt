package com.example.carpool

import com.example.carpool.utils.calculateDistance
import com.example.carpool.utils.isCurrentTimeInRange
import com.example.carpool.utils.createRide
import com.example.carpool.utils.getTravelTime
import com.google.android.gms.maps.model.LatLng as GmsLatLng
import com.google.maps.model.LatLng as MapsLatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

fun fetchAvailableDrivers(
    apiKey: String,
    source: GmsLatLng,
    destination: GmsLatLng,
    callback: (List<Map<String, Any>>, Any?) -> Unit,
) {
    val sourceMapsLatLng = MapsLatLng(source.latitude, source.longitude)
    val destinationMapsLatLng = MapsLatLng(destination.latitude, destination.longitude)

    getTravelTime(apiKey, sourceMapsLatLng, destinationMapsLatLng) { travelTime ->
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .whereEqualTo("userType", "driver")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val drivers = result.documents.mapNotNull { document ->
                    val name = document.getString("name")
                    val rating = document.getDouble("rating")?.toFloat()
                    val price = document.getDouble("price")
                    val workTime = document.getString("workTime")
                    val serviceLocation = document.get("serviceLocation") as? Map<String, Any>

                    // print all the driver info
                    println("Driver Info: $name, $rating, $price, $workTime, $serviceLocation")

                    // print is in range calculation
                    println("Work Time: $workTime")
                    println("Is in range: ${workTime?.let { isCurrentTimeInRange(it) }}")

                    if (
                        name != null && rating != null && price != null &&
                        workTime != null && serviceLocation != null &&
                        isCurrentTimeInRange(workTime)
                    ) {
                        val driverLat = serviceLocation["latitude"] as? Double
                        val driverLon = serviceLocation["longitude"] as? Double
                        val radius = serviceLocation["radius"] as? Number
                        
                        val serviceRadius = radius?.toDouble()

                        println("Driver Location: $driverLat, $driverLon, $serviceRadius")
                        
                        if (driverLat != null && driverLon != null && serviceRadius != null) {
                            val distanceToSource = calculateDistance(source.latitude, source.longitude, driverLat, driverLon)
                            val distanceToDestination = calculateDistance(destination.latitude, destination.longitude, driverLat, driverLon)

                            println("Distances: $distanceToSource, $distanceToDestination")
                            
                            if (distanceToSource <= serviceRadius && distanceToDestination <= serviceRadius) {
                                mapOf(
                                    "id" to document.id, // Add driver ID
                                    "name" to name,
                                    "rating" to rating,
                                    "price" to price
                                )
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                callback(drivers, travelTime)
            }
        }
    }

// Function to handle driver selection and create a ride
fun selectDriverAndCreateRide(
    driver: Map<String, Any>,
    source: GmsLatLng,
    destination: GmsLatLng,
    passengerId: String,
    callback: (Boolean, String?) -> Unit
) {
    val driverId = driver["id"] as String
    val sourceMap = mapOf("latitude" to source.latitude, "longitude" to source.longitude)
    val destinationMap = mapOf("latitude" to destination.latitude, "longitude" to destination.longitude)
    
    createRide(sourceMap, destinationMap, driverId, passengerId) { success, rideId ->
        if (success && !rideId.isNullOrEmpty()) {
            // Log the rideId for debugging purposes
            println("Ride created successfully with rideId: $rideId")
        } else {
            println("Failed to create ride or received empty rideId")
        }
        callback(success, rideId)
    }
}