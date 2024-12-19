package com.example.carpool.utils

import com.google.maps.GeoApiContext
import com.google.maps.DistanceMatrixApi
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode

fun getTravelTime(apiKey: String, origin: LatLng, destination: LatLng, callback: (String) -> Unit) {
    val context = GeoApiContext.Builder()
        .apiKey(apiKey)
        .build()

    DistanceMatrixApi.newRequest(context)
        .origins(com.google.maps.model.LatLng(origin.lat, origin.lng))
        .destinations(com.google.maps.model.LatLng(destination.lat, destination.lng))
        .mode(TravelMode.DRIVING)
        .awaitIgnoreError()
        ?.let { result ->
            val duration = result.rows[0].elements[0].duration
            callback(duration.humanReadable)
        }
}