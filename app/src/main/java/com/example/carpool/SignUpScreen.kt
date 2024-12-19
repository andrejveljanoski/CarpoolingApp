package com.example.carpool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SignUpScreen(auth: FirebaseAuth, modifier: Modifier = Modifier, onLoginClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("Passenger") }
    var vehicleManufacturer by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    val firestore = FirebaseFirestore.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Choose the type of account:")
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = accountType == "Passenger",
                onClick = { accountType = "Passenger" }
            )
            Text("Passenger", modifier = Modifier.padding(start = 1.dp))

            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = accountType == "Driver",
                onClick = { accountType = "Driver" }
            )
            Text("Driver", modifier = Modifier.padding(start = 1.dp))
        }
        if (accountType == "Driver") {
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = vehicleManufacturer,
                onValueChange = { vehicleManufacturer = it },
                label = { Text("Vehicle Manufacturer") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = vehicleModel,
                onValueChange = { vehicleModel = it },
                label = { Text("Vehicle Model") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = licensePlate,
                onValueChange = { licensePlate = it },
                label = { Text("License Plate") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (accountType == "Driver" && (vehicleManufacturer.isEmpty() || vehicleModel.isEmpty() || licensePlate.isEmpty())) {
                        message = "Vehicle information must not be empty for drivers"
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = task.result?.user
                                    val userData = hashMapOf(
                                        "id" to user?.uid,
                                        "rating" to 0.0,
                                        "earnings" to 0.0,
                                        "drivesCompleted" to 0,
                                        "name" to fullName,
                                        "email" to email,
                                        "workTime" to "9:00 - 17:00",
                                        "price" to 10.0 ,// Default price
                                        "userType" to if (accountType == "Passenger") "passenger" else "driver"
                                    )
                                    if( accountType == "Driver") {
                                        val driverData = hashMapOf(
                                            "vehicleManufacturer" to vehicleManufacturer,
                                            "vehicleModel" to vehicleModel,
                                            "licensePlate" to licensePlate
                                        )
                                       userData.putAll(driverData)
                                    }
                                    val driverData = hashMapOf(
                                        "vehicleManufacturer" to vehicleManufacturer,
                                        "vehicleModel" to vehicleModel,
                                        "licensePlate" to licensePlate
                                    )
                                    firestore.collection("users").document(user?.uid ?: "").set(userData)
                                        .addOnSuccessListener {
                                            message = "Sign-Up successful as $accountType"
                                        }
                                        .addOnFailureListener { e ->
                                            message = "Sign-Up failed: ${e.message}"
                                        }
                                } else {
                                    message = "Sign-Up failed: ${task.exception?.message}"
                                }
                            }
                    }
                } else {
                    message = "Fill every field please"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Already have an account?", modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            text = "Login",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onLoginClick() }
        )
        Text(text = message)
    }
}