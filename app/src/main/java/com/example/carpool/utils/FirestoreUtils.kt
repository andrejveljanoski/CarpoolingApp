package com.example.carpool.utils

import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime

data class Ride(
    val source: Map<String, Double>,
    val destination: Map<String, Double>,
    val driverId: String,
    val passengerId: String,
    val rideTime: String // Added rideTime field
)

fun createRide(
    source: Map<String, Double>,
    destination: Map<String, Double>,
    driverId: String,
    passengerId: String,
    callback: (Boolean, String?) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val ride = Ride(
        source,
        destination,
        driverId,
        passengerId,
        LocalDateTime.now().toString() // Hardcode to current time
    )
    firestore.collection("rides").add(ride)
        .addOnSuccessListener {
            callback(true, it.id)
        }
        .addOnFailureListener { e ->
            callback(false, e.message)
        }
}