package com.example.carpool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.carpool.ui.theme.CarpoolTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val apiKey = "AIzaSyDI4mmRhB89ujP-Gv03UVEVxZSjtn_BR2A" // Replace with your actual API key
        setContent {
            CarpoolTheme {
                val navController = rememberNavController()
                var userRole by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(auth.currentUser) {
                    auth.currentUser?.let { user ->
                        firestore.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                userRole = document.getString("role")
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } ?: run {
                        isLoading = false
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
                } else {
                    val startDestination = if (auth.currentUser != null && userRole != null) {
                        when (userRole) {
                            "passenger" -> "passengerDashboard"
                            "driver" -> "driverDashboard"
                            else -> "login"
                        }
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login") {
                            LoginScreen(
                                auth = auth,
                                onSignUpClick = { navController.navigate("signup") },
                                onDashboardClick = {
                                    firestore.collection("users").document(auth.currentUser!!.uid).get()
                                        .addOnSuccessListener { document ->
                                            userRole = document.getString("userType")
                                            when (userRole) {
                                                "passenger" -> navController.navigate("passengerDashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                                "driver" -> navController.navigate("driverDashboard") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                                else -> navController.navigate("login")
                                            }
                                        }
                                }
                            )
                        }
                        composable("signup") {
                            SignUpScreen(
                                auth = auth,
                                onLoginClick = { navController.navigate("login") }
                            )
                        }
                        composable("driverDashboard") {
                            DriverDashboardScreen(
                                auth = auth,
                                onLogout = { navController.navigate("login") },
                                onSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("passengerDashboard") {
                            PassengerDashboardScreen(
                                auth = auth,
                                apiKey = apiKey,
                                onLogout = { navController.navigate("login") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                auth = auth,
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}