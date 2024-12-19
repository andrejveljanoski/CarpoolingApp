package com.example.carpool.utils

import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class FirestoreUtilsTest {

    @Test
    fun testCreateRideSuccess() {
        // Mock Firestore components
        val firestore = mock<FirebaseFirestore>()
        val collection = mock<CollectionReference>()
        val documentRef = mock<DocumentReference>()

        whenever(firestore.collection("rides")).thenReturn(collection)
        whenever(collection.add(any())).thenReturn(Tasks.forResult(documentRef))
        whenever(documentRef.id).thenReturn("ride123")

        // Replace Firestore instance with mock
        // This requires dependency injection in actual code. For simplicity, this is illustrative.

        val source = mapOf("latitude" to 40.7128, "longitude" to -74.0060)
        val destination = mapOf("latitude" to 34.0522, "longitude" to -118.2437)
        val driverId = "driver123"
        val passengerId = "passenger456"

        // Function to test
        createRide(source, destination, driverId, passengerId) { success, rideId ->
            assertTrue(success)
            assertEquals("ride123", rideId)
        }
    }

    @Test
    fun testCreateRideFailure() {
        // Mock Firestore components
        val firestore = mock<FirebaseFirestore>()
        val collection = mock<CollectionReference>()
        whenever(firestore.collection("rides")).thenReturn(collection)
        whenever(collection.add(any())).thenReturn(Tasks.forException(Exception("Failed to add ride")))

        val source = mapOf("latitude" to 40.7128, "longitude" to -74.0060)
        val destination = mapOf("latitude" to 34.0522, "longitude" to -118.2437)
        val driverId = "driver123"
        val passengerId = "passenger456"

        createRide(source, destination, driverId, passengerId) { success, rideId ->
            assertFalse(success)
            assertEquals("Failed to add ride", rideId)
        }
    }
}