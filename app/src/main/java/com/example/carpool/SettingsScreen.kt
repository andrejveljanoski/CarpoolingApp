package com.example.carpool

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    auth: FirebaseAuth,
    onBack: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    var vehicleManufacturer by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var price by remember { mutableStateOf("10.0") } // Default price

    // Fetch user data from Firestore
    LaunchedEffect(user) {
        user?.let {
            firestore.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        vehicleManufacturer = document.getString("vehicleManufacturer") ?: ""
                        vehicleModel = document.getString("vehicleModel") ?: ""
                        licensePlate = document.getString("licensePlate") ?: ""
                        price = document.getDouble("price")?.toString() ?: "10.0"
                    }
                }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                DrawerContent(onItemClick = { destination ->
                    scope.launch {
                        drawerState.close()
                    }
                    when (destination) {
                        "dashboard" -> {
                            onBack()
                        }
                        "settings" -> {
                            // Already on settings
                        }

                    }
                })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Update Vehicle Details")
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
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        user?.let {
                            val userData = hashMapOf(
                                "vehicleManufacturer" to vehicleManufacturer,
                                "vehicleModel" to vehicleModel,
                                "licensePlate" to licensePlate,
                                "price" to price.toInt(),
                            )
                            firestore.collection("users").document(it.uid).set(userData, SetOptions.merge())
                                .addOnSuccessListener {
                                    message = "Vehicle details updated successfully"
                                }
                                .addOnFailureListener { e ->
                                    message = "Error updating vehicle details: ${e.message}"
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}