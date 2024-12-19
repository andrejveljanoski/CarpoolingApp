package com.example.carpool

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.example.carpool.fetchDrivers
import androidx.compose.runtime.LaunchedEffect
import com.example.carpool.ui.theme.Ride
import com.google.firebase.firestore.Query
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDashboardScreen(
    auth: FirebaseAuth,
    apiKey: String,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val context = LocalContext.current
    var rating by remember { mutableStateOf(0.0f) }
    var showRidePicker by remember { mutableStateOf(false) }
    var showAvailableDrivers by remember { mutableStateOf(false) }
    var availableDrivers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var lastDrives by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var travelTimeText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(LatLng(41.9981, 21.4254)) }
    var destination by remember { mutableStateOf(LatLng(41.9981, 21.4254)) }


    val imgLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    // Fetch user data or other necessary data for passenger
    LaunchedEffect(user) {
        user?.let {
            firestore.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        rating = document.getDouble("rating")?.toFloat() ?: 0.0f
                    }
                }
        }
    }

    // Fetch user data or other necessary data for passenger
    LaunchedEffect(user) {
        user?.let {
            firestore.collection("rides")
                .whereEqualTo("passengerId", it.uid)
                .orderBy("rideTime", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { documents ->
                    println("DOCUMENTS: $documents")
                    if(documents.isEmpty) {
                        lastDrives = emptyList()
                        return@addOnSuccessListener
                    }
                    val rides = documents.toObjects(Ride::class.java)
                    lastDrives = rides
                    println("RIDES $rides")
                }
                .addOnFailureListener{ e -> println("Error getting documents: $e") }
        }
    }

    if (showRidePicker) {
        RidePickerDialog(
            onLocationSelected = { src, des, _, _ ->
                showRidePicker = false
                source = src
                destination = des
                scope.launch {
                    val (drivers, travelTime) = fetchDrivers(apiKey, source, destination)
                    availableDrivers = drivers
                    showAvailableDrivers = true
                    travelTimeText = "Estimated travel time: $travelTime"
                }
            },
            onDismissRequest = { showRidePicker = false }
        )
    }
    if (showAvailableDrivers) {
        AvailableDriversDialog(
            availableDrivers = availableDrivers,
            onDriverSelected = { driver ->
                selectDriverAndCreateRide(driver, source, destination, user!!.uid) { _, _ ->
                    showAvailableDrivers = false
                }
            },
            onDismissRequest = { showAvailableDrivers = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                PassengerDrawerContent(onItemClick = { destination ->
                    scope.launch {
                        drawerState.close()
                    }
                    when (destination) {
                        "dashboard" -> {
                            // Already on dashboard
                        }
                        // Add other destinations if needed
                    }
                })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(90.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                        }
                        ProfileDropdownMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            onLogout = {
                                auth.signOut()
                                onLogout()
                            }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dashboard Title
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Dashboard",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Start
                    )
                }

                // Rating Box with .rating GIF
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Rating: $rating / 5.0 ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.W600,
                            modifier = Modifier.weight(1f)
                        )
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.rating,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Rating",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Find a ride Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable{showRidePicker = true}
                        .padding(16.dp)

                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Find a Ride",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.map,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Map Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Last Drives Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Last Rides",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (lastDrives.isNotEmpty()) {
                                lastDrives.forEach { ride ->
                                    Text(
                                        text = "${LocalDateTime.parse(ride.rideTime).hour}:${LocalDateTime.parse(ride.rideTime).minute}",
                                        fontSize = 18.sp
                                    )
                                }
                            } else {
                                Text(
                                    "No recent rides.",
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.wheel,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Wheel Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = travelTimeText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AvailableDriversDialog(
    availableDrivers: List<Map<String, Any>>,
    onDriverSelected: (Map<String, Any>) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedDriverIndex by remember { mutableStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Available Drivers") },
        text = {
            LazyColumn {
                itemsIndexed(availableDrivers) { index, driver ->
                    val isSelected = selectedDriverIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { selectedDriverIndex = index }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.inversePrimary
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${driver["name"]}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rating: ${driver["rating"]}   Price: ${driver["price"]}",
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedDriverIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedDriverIndex >= 0) {
                        onDriverSelected(availableDrivers[selectedDriverIndex])
                        onDismissRequest()
                    }
                },
                enabled = selectedDriverIndex >= 0
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun RidePickerDialog(
    onLocationSelected: (LatLng, LatLng, String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var sourceLat by remember { mutableStateOf("41.9981") } // Default to Skopje
    var sourceLng by remember { mutableStateOf("21.4254") } // Default to Skopje
    var destinationLat by remember { mutableStateOf("41.9981") } // Default to Skopje
    var destinationLng by remember { mutableStateOf("21.4254") } // Default to Skopje
    var destination by remember { mutableStateOf(LatLng(41.9981, 21.4254)) } // Default to Skopje
    var mapLoaded by remember { mutableStateOf(false) }
    var timeOfTravel by remember { mutableStateOf("") }
    var lengthOfTravel by remember { mutableStateOf("") }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destination, 10f)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Select Ride Locations") },
        text = {
            Column {
                // Input for source latitude and longitude
                Row {
                    OutlinedTextField(
                        value = sourceLat,
                        onValueChange = { sourceLat = it },
                        label = { Text("Source Latitude") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = sourceLng,
                        onValueChange = { sourceLng = it },
                        label = { Text("Source Longitude") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Input for destination latitude and longitude
                Row {
                    OutlinedTextField(
                        value = destinationLat,
                        onValueChange = { destinationLat = it },
                        label = { Text("Destination Latitude") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = destinationLng,
                        onValueChange = { destinationLng = it },
                        label = { Text("Destination Longitude") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Map to select destination
                GoogleMap(
                    modifier = Modifier.height(200.dp),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = { mapLoaded = true },
                    onMapClick = { latLng ->
                        destination = latLng
                        destinationLat = latLng.latitude.toString()
                        destinationLng = latLng.longitude.toString()
                    }
                ) {
                    if (mapLoaded) {
                        Marker(
                            state = rememberMarkerState(position = destination),
                            title = "Destination"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sourceLatLng = LatLng(
                    sourceLat.toDoubleOrNull() ?: 41.9981,
                    sourceLng.toDoubleOrNull() ?: 21.4254
                )
                val destinationLatLng = LatLng(
                    destinationLat.toDoubleOrNull() ?: 41.9981,
                    destinationLng.toDoubleOrNull() ?: 21.4254
                )
                onLocationSelected(sourceLatLng, destinationLatLng, timeOfTravel, lengthOfTravel)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PassengerDrawerContent(onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Dashboard",
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick("dashboard") }
                .padding(16.dp)
        )
        // Add other menu items if needed
    }
}

@Composable
fun ProfileDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Logout") },
            onClick = {
                onLogout()
                onDismiss()
            }
        )
    }
}