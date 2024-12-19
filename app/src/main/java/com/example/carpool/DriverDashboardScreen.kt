package com.example.carpool

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(
    auth: FirebaseAuth,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onSettings: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    var rating by remember { mutableStateOf(0.0f) }
    var earnings by remember { mutableStateOf(0.0f) }
    var drivesCompleted by remember { mutableStateOf(0) }
    var vehicleManufacturer by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var workTime by remember { mutableStateOf("9:00  - 17:00 ") }
    var showTimePicker by remember { mutableStateOf(false) }
    val lastDrives = listOf("Drive 1", "Drive 2", "Drive 3") // Placeholder data
    var showLocationPicker by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf(LatLng(41.9973, 21.4280)) }
    var radius by remember { mutableStateOf(10.0) } // Default radius in km
    var price by remember { mutableStateOf(10.0) }

    val context = LocalContext.current
    val imgLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    // Fetch user data from Firestore
    LaunchedEffect(user) {
        user?.let {
            firestore.collection("users").document(it.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        rating = document.getDouble("rating")?.toFloat() ?: 0.0f
                        earnings = document.getDouble("earnings")?.toFloat() ?: 0.0f
                        drivesCompleted = document.getDouble("drivesCompleted")?.toInt() ?: 0
                        vehicleManufacturer = document.getString("vehicleManufacturer") ?: ""
                        vehicleModel = document.getString("vehicleModel") ?: ""
                        licensePlate = document.getString("licensePlate") ?: ""
                        workTime = document.getString("workTime") ?: "9:00  - 17:00 "
                        price = document.getDouble("price") ?: 10.0 // Fetch price
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
                            // Already on dashboard
                        }
                        "settings" -> {
                            onSettings()
                        }
                        "logout" -> {
                            auth.signOut()
                            onLogout()
                        }
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
                        Image(
                            painter = painterResource(id = R.drawable.placeholder),
                            contentDescription = "Vehicle",
                            modifier = Modifier.size(80.dp)
                        )
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
                            onSettings = onSettings,
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

                // Analytics Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Analytics",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Drives Completed: $drivesCompleted",
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Earnings: $$earnings",
                                fontSize = 18.sp
                            )
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.chart,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Analytics",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Work Time Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Work Time",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = workTime,
                                fontSize = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Set Work Time",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.time,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Work Time Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
// Location Radius Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Location Radius",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Lat: ${selectedLocation.latitude}, Lon: ${selectedLocation.longitude}",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Radius: ${radius} km",
                                fontSize = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            IconButton(onClick = { showLocationPicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Set Map Radius",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = R.drawable.radius,
                                imageLoader = imgLoader
                            ),
                            contentDescription = "Map Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                if (showTimePicker) {
                    TimePickerDialog(
                        onTimeSelected = { startTime, endTime ->
                            workTime = "$startTime - $endTime"
                            showTimePicker = false

                            // Update Firestore
                            user?.let {
                                val data = mapOf("workTime" to workTime)
                                firestore.collection("users").document(it.uid)
                                    .set(data, SetOptions.merge())
                            }
                        },
                        onDismissRequest = { showTimePicker = false }
                    )
                }
                if (showLocationPicker) {
                    LocationPickerDialog(
                        initialLocation = selectedLocation,
                        initialRadius = radius,
                        onLocationSelected = { location, newRadius ->
                            selectedLocation = location
                            radius = newRadius
                            showLocationPicker = false

                            // Update Firestore
                            user?.let {
                                val data = mapOf(
                                    "serviceLocation" to mapOf(
                                        "latitude" to location.latitude,
                                        "longitude" to location.longitude
                                    ),
                                    "serviceRadius" to newRadius
                                )
                                firestore.collection("users").document(it.uid)
                                    .set(data, SetOptions.merge())
                            }
                        },
                        onDismissRequest = { showLocationPicker = false }
                    )
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
                                "Last Drives",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.W600
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (lastDrives.isNotEmpty()) {
                                lastDrives.forEach { drive ->
                                    Text(
                                        text = drive,
                                        fontSize = 18.sp
                                    )
                                }
                            } else {
                                Text(
                                    "No recent drives.",
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
                    }
                }
                }
            }
        }
    }

@Composable
fun TimePickerDialog(
    onTimeSelected: (String, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var startTime by remember { mutableStateOf("9:00 ") }
    var endTime by remember { mutableStateOf("17:00 ") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Select Work Time") },
        text = {
            Column {
                // You can replace these TextFields with actual TimePicker components
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time") }
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(startTime, endTime) }) {
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
fun DrawerContent(onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Dashboard",
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick("dashboard") }
                .padding(16.dp)
        )
        Text(
            text = "Settings",
            fontSize = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick("settings") }
                .padding(16.dp)
        )
    }
}
@Composable
fun LocationPickerDialog(
    initialLocation: LatLng,
    initialRadius: Double,
    onLocationSelected: (LatLng, Double) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var radius by remember { mutableStateOf(initialRadius) }
    var latitude by remember { mutableStateOf(initialLocation.latitude.toString()) }
    var longitude by remember { mutableStateOf(initialLocation.longitude.toString()) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Select Service Area") },
        text = {
            Column {
                // Map to select location
                GoogleMap(
                    modifier = Modifier.height(200.dp),
                    cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(initialLocation, 10f)
                    },
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        latitude = latLng.latitude.toString()
                        longitude = latLng.longitude.toString()
                    }
                ) {
                    Marker(
                        state = rememberMarkerState(position = selectedLocation)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Input for latitude and longitude in a row
                Row {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Input for radius
                OutlinedTextField(
                    value = radius.toString(),
                    onValueChange = { radius = it.toDoubleOrNull() ?: 10.0 },
                    label = { Text("Service Radius (km)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lat = latitude.toDoubleOrNull() ?: initialLocation.latitude
                val lon = longitude.toDoubleOrNull() ?: initialLocation.longitude
                onLocationSelected(LatLng(lat, lon), radius)
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
fun ProfileDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 65.dp, y = -5.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                onSettings()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Logout") },
            onClick = {
                onLogout()
                onDismiss()
            }
        )
    }
}