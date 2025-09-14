package com.lifttogether

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lifttogether.repository.FirebaseRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class UrgencyLevel(val displayName: String) {
    EMERGENCY("Emergency"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

data class RideRequest(
    val pickupLocation: String,
    val dropoffLocation: String,
    val urgency: UrgencyLevel,
    val notes: String = "",
    val id: String = java.util.UUID.randomUUID().toString()
)

data class Volunteer(
    val name: String,
    val vehicleInfo: VehicleInfo,
    val rating: Float = 4.5f,
    val isAvailable: Boolean = false,
    val id: String = ""
)

data class VehicleInfo(
    val make: String,
    val model: String,
    val color: String,
    val licensePlate: String
)

data class RideStatus(
    val request: RideRequest,
    val volunteer: Volunteer?,
    val eta: String,
    val status: RideStatusType
)

enum class RideStatusType {
    PENDING,
    ASSIGNED,
    ON_THE_WAY,
    ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Firebase only if google-services.json is valid
            try {
                FirebaseApp.initializeApp(this)
            } catch (e: Exception) {
                // If Firebase fails, continue without it
                android.util.Log.w("LiftTogether", "Firebase initialization failed: ${e.message}")
            }
            
            setContent {
                LiftTogetherTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LiftTogetherApp()
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback if Firebase fails
            setContent {
                LiftTogetherTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LiftTogether",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "App is starting up...",
                                fontSize = 16.sp,
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Error: ${e.message}",
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiftTogetherApp() {
    var userType by remember { mutableStateOf("") }
    var isSignedIn by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    val firebaseRepository = remember { FirebaseRepository() }
    val currentUser = firebaseRepository.getCurrentUser()

    // Check if user is already signed in
    LaunchedEffect(Unit) {
        if (currentUser != null && userType.isEmpty()) {
            // Get user type from Firestore
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get().await()
                
                val userTypeFromDB = userDoc.getString("userType") ?: "Rider"
                userType = userTypeFromDB
                isSignedIn = true
            } catch (e: Exception) {
                // If we can't get user type, default to Rider
                userType = "Rider"
                isSignedIn = true
            }
        }
    }

    if (!isSignedIn) {
        if (showAuth) {
            AuthScreen(
                onBack = { showAuth = false },
                onSignIn = { type ->
                    userType = type
                    isSignedIn = true
                    showAuth = false
                }
            )
        } else {
            LoginScreen(
                onShowAuth = { showAuth = true }
            )
        }
    } else {
        MainScreen(
            userType = userType,
            onBack = {
                firebaseRepository.signOut()
                isSignedIn = false
                userType = ""
            }
        )
    }
}

@Composable
fun LoginScreen(
    onShowAuth: () -> Unit
) {
    val firebaseRepository = remember { FirebaseRepository() }
    val currentUser = firebaseRepository.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LiftTogether",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connecting riders with volunteer drivers",
            fontSize = 16.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (currentUser != null) {
            Text(
                text = "Welcome back, ${currentUser.email}!",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "You are already signed in. Please use the Sign Up / Log In button to access your account.",
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            Text(
                text = "Please sign in to continue:",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (currentUser != null) "Access your account:" else "Already have an account?",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextButton(
            onClick = onShowAuth,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up / Log In")
        }
    }
}

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onSignIn: (String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var emailPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val firebaseRepository = remember { FirebaseRepository() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = if (isSignUp) "Sign Up" else "Log In",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Auth Form
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Toggle between Sign Up and Log In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { 
                            isSignUp = false
                            errorMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSignUp) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text("Log In")
                    }
                    Button(
                        onClick = { 
                            isSignUp = true
                            errorMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSignUp) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text("Sign Up")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // User Type Selection
                Text(
                    text = "I am a:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { userType = "Rider" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userType == "Rider") MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text("Rider")
                    }
                    Button(
                        onClick = { userType = "Volunteer" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (userType == "Volunteer") MaterialTheme.colorScheme.secondary else androidx.compose.ui.graphics.Color.Transparent
                        )
                    ) {
                        Text("Volunteer")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Email/Phone Field
                OutlinedTextField(
                    value = emailPhone,
                    onValueChange = { emailPhone = it },
                    label = { Text("Email or Phone Number") },
                    placeholder = { Text("Enter email or phone") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("Enter password") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "👁️" else "👁️‍🗨️")
                        }
                    }
                )
                
                // Confirm Password (only for Sign Up)
                if (isSignUp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Confirm password") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Text(if (showConfirmPassword) "👁️" else "👁️‍🗨️")
                            }
                        }
                    )
                }
                
                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = androidx.compose.ui.graphics.Color.Red,
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                        // Submit Button
                        Button(
                            onClick = {
                                if (validateForm(isSignUp, emailPhone, password, confirmPassword, userType)) {
                                    isLoading = true
                                    errorMessage = ""
                                    
                                    CoroutineScope(Dispatchers.Main).launch {
                                        try {
                                            if (isSignUp) {
                                                // Sign Up
                                                val result = firebaseRepository.signUp(emailPhone, password, userType)
                                                if (result.isSuccess) {
                                                    onSignIn(userType)
                                                } else {
                                                    errorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                                                }
                                            } else {
                                                // Sign In
                                                val result = firebaseRepository.signIn(emailPhone, password)
                                                if (result.isSuccess) {
                                                    // Get user type from Firestore
                                                    val userDoc = FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(result.getOrNull() ?: "")
                                                        .get().await()
                                                    
                                                    val userTypeFromDB = userDoc.getString("userType") ?: userType
                                                    onSignIn(userTypeFromDB)
                                                } else {
                                                    errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Authentication failed"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    errorMessage = getValidationError(isSignUp, emailPhone, password, confirmPassword, userType)
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isLoading) "Please wait..." else if (isSignUp) "Sign Up" else "Log In")
                        }
                
                // Forgot Password (only for Log In)
                if (!isSignUp) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { /* Handle forgot password */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Forgot Password?")
                    }
                }
            }
        }
    }
}

fun validateForm(
    isSignUp: Boolean,
    emailPhone: String,
    password: String,
    confirmPassword: String,
    userType: String
): Boolean {
    return when {
        userType.isEmpty() -> false
        emailPhone.isEmpty() -> false
        password.isEmpty() -> false
        isSignUp && confirmPassword.isEmpty() -> false
        isSignUp && password != confirmPassword -> false
        password.length < 6 -> false
        else -> true
    }
}

fun getValidationError(
    isSignUp: Boolean,
    emailPhone: String,
    password: String,
    confirmPassword: String,
    userType: String
): String {
    return when {
        userType.isEmpty() -> "Please select your role (Rider or Volunteer)"
        emailPhone.isEmpty() -> "Please enter your email or phone number"
        password.isEmpty() -> "Please enter your password"
        isSignUp && confirmPassword.isEmpty() -> "Please confirm your password"
        isSignUp && password != confirmPassword -> "Passwords do not match"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> ""
    }
}

@Composable
fun MainScreen(userType: String, onBack: () -> Unit) {
    val firebaseRepository = remember { FirebaseRepository() }
    val currentUser = firebaseRepository.getCurrentUser()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Welcome, $userType!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Show current user email and sign out button
        if (currentUser != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logged in as: ${currentUser.email}",
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
                TextButton(
                    onClick = {
                        firebaseRepository.signOut()
                        onBack()
                    }
                ) {
                    Text("Sign Out")
                }
            }
        }
        
        if (userType == "Rider") {
            RiderScreen()
        } else {
            VolunteerScreen()
        }
    }
}

@Composable
fun RiderScreen() {
    var rideRequests by remember { mutableStateOf(listOf<RideRequest>()) }
    var showRequestForm by remember { mutableStateOf(false) }
    var currentRideStatus by remember { mutableStateOf<RideStatus?>(null) }
    
    if (currentRideStatus != null) {
        RideStatusScreen(
            rideStatus = currentRideStatus!!,
            onBack = { currentRideStatus = null },
            onCancel = { 
                currentRideStatus = currentRideStatus?.copy(status = RideStatusType.CANCELLED)
            }
        )
    } else {
        Column {
            Text(
                text = "Your Ride Requests:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (rideRequests.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "No ride requests yet. Tap 'Request New Ride' to get started!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn {
                    items(rideRequests) { request ->
                        RideRequestCard(
                            request = request,
                            onClick = { 
                                // Create a mock ride status for demonstration
                                val mockVolunteer = Volunteer(
                                    name = "John Smith",
                                    vehicleInfo = VehicleInfo(
                                        make = "Toyota",
                                        model = "Camry",
                                        color = "Silver",
                                        licensePlate = "ABC-123"
                                    ),
                                    rating = 4.8f
                                )
                                currentRideStatus = RideStatus(
                                    request = request,
                                    volunteer = mockVolunteer,
                                    eta = "8 minutes",
                                    status = RideStatusType.ASSIGNED
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showRequestForm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request New Ride")
            }
            
            // Ride request form
            if (showRequestForm) {
                RequestRideDialog(
                    onDismiss = { showRequestForm = false },
                    onRequestSubmitted = { request ->
                        rideRequests = rideRequests + request
                        showRequestForm = false
                        
                        // Create a mock ride status for demonstration
                        val mockVolunteer = Volunteer(
                            name = "Sarah Johnson",
                            vehicleInfo = VehicleInfo(
                                make = "Honda",
                                model = "Civic",
                                color = "Blue",
                                licensePlate = "XYZ-789"
                            ),
                            rating = 4.9f
                        )
                        currentRideStatus = RideStatus(
                            request = request,
                            volunteer = mockVolunteer,
                            eta = "12 minutes",
                            status = RideStatusType.ASSIGNED
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun RideRequestCard(
    request: RideRequest,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.urgency.displayName,
                    fontWeight = FontWeight.Bold,
                    color = when (request.urgency) {
                        UrgencyLevel.EMERGENCY -> androidx.compose.ui.graphics.Color.Red
                        UrgencyLevel.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                        UrgencyLevel.MEDIUM -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                        UrgencyLevel.LOW -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pickup",
                    tint = androidx.compose.ui.graphics.Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "From: ${request.pickupLocation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Dropoff",
                    tint = androidx.compose.ui.graphics.Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "To: ${request.dropoffLocation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (request.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${request.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
        }
    }
}

@Composable
fun RequestRideDialog(
    onDismiss: () -> Unit,
    onRequestSubmitted: (RideRequest) -> Unit
) {
    var pickupLocation by remember { mutableStateOf("") }
    var dropoffLocation by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf(UrgencyLevel.MEDIUM) }
    var notes by remember { mutableStateOf("") }
    var useGPS by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request a Ride") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // GPS Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useGPS,
                        onCheckedChange = { useGPS = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use GPS for pickup location")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pickup Location
                OutlinedTextField(
                    value = pickupLocation,
                    onValueChange = { pickupLocation = it },
                    label = { Text("Pickup Location") },
                    placeholder = { Text(if (useGPS) "GPS will auto-detect" else "Enter pickup address") },
                    enabled = !useGPS,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pickup"
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dropoff Location
                OutlinedTextField(
                    value = dropoffLocation,
                    onValueChange = { dropoffLocation = it },
                    label = { Text("Drop-off Location") },
                    placeholder = { Text("Enter destination address") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Dropoff"
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Urgency Level
                Text(
                    text = "Urgency Level:",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                UrgencyLevel.values().forEach { urgency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedUrgency == urgency),
                                onClick = { selectedUrgency = urgency }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedUrgency == urgency),
                            onClick = { selectedUrgency = urgency }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = urgency.displayName,
                            color = when (urgency) {
                                UrgencyLevel.EMERGENCY -> androidx.compose.ui.graphics.Color.Red
                                UrgencyLevel.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                UrgencyLevel.MEDIUM -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                                UrgencyLevel.LOW -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Optional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g., wheelchair needed, special requirements") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val request = RideRequest(
                        pickupLocation = if (useGPS) "Current Location (GPS)" else pickupLocation,
                        dropoffLocation = dropoffLocation,
                        urgency = selectedUrgency,
                        notes = notes
                    )
                    onRequestSubmitted(request)
                },
                enabled = dropoffLocation.isNotEmpty() && (!useGPS && pickupLocation.isNotEmpty() || useGPS)
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RideStatusScreen(
    rideStatus: RideStatus,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Ride Status",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (rideStatus.status) {
                            RideStatusType.PENDING -> "Searching for volunteer..."
                            RideStatusType.ASSIGNED -> "Volunteer assigned"
                            RideStatusType.ON_THE_WAY -> "On the way"
                            RideStatusType.ARRIVED -> "Arrived at pickup"
                            RideStatusType.IN_PROGRESS -> "Ride in progress"
                            RideStatusType.COMPLETED -> "Ride completed"
                            RideStatusType.CANCELLED -> "Ride cancelled"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rideStatus.eta,
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Volunteer Info
                if (rideStatus.volunteer != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Volunteer",
                            tint = androidx.compose.ui.graphics.Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = rideStatus.volunteer.name,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = androidx.compose.ui.graphics.Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${rideStatus.volunteer.rating}",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Vehicle Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚗",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${rideStatus.volunteer.vehicleInfo.color} ${rideStatus.volunteer.vehicleInfo.make} ${rideStatus.volunteer.vehicleInfo.model}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "License: ${rideStatus.volunteer.vehicleInfo.licensePlate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        }
        
        // Map View Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🗺️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Map View",
                        style = MaterialTheme.typography.headlineSmall,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                    Text(
                        text = "OpenStreetMap integration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        }
        
        // Route Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Route Details",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pickup",
                        tint = androidx.compose.ui.graphics.Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "From: ${rideStatus.request.pickupLocation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Dropoff",
                        tint = androidx.compose.ui.graphics.Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "To: ${rideStatus.request.dropoffLocation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (rideStatus.request.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${rideStatus.request.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        }
        
        // Cancel Button
        if (rideStatus.status != RideStatusType.COMPLETED && rideStatus.status != RideStatusType.CANCELLED) {
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Cancel Ride")
            }
        }
    }
}

@Composable
fun VolunteerScreen() {
    var isAvailable by remember { mutableStateOf(false) }
    var currentRide by remember { mutableStateOf<RideStatus?>(null) }
    var rideRequests by remember { 
        mutableStateOf(listOf(
            RideRequest(
                pickupLocation = "123 Main St, Downtown",
                dropoffLocation = "City Hospital",
                urgency = UrgencyLevel.EMERGENCY,
                notes = "Wheelchair needed"
            ),
            RideRequest(
                pickupLocation = "456 Oak Ave, Suburbs",
                dropoffLocation = "Grocery Store",
                urgency = UrgencyLevel.MEDIUM,
                notes = "Regular pickup"
            ),
            RideRequest(
                pickupLocation = "789 Pine St, Uptown",
                dropoffLocation = "Airport",
                urgency = UrgencyLevel.HIGH,
                notes = "Flight at 3 PM"
            )
        ))
    }
    
    if (currentRide != null) {
        VolunteerRideStatusScreen(
            rideStatus = currentRide!!,
            onBack = { currentRide = null },
            onStatusUpdate = { newStatus ->
                currentRide = currentRide?.copy(status = newStatus)
            }
        )
    } else {
        Column {
            // Availability Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available to give rides",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isAvailable) "You're online and ready to help!" else "You're offline",
                            fontSize = 14.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }
            }
            
            if (isAvailable) {
                Text(
                    text = "Available Ride Requests:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (rideRequests.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "No ride requests available at the moment. Stay online to help when requests come in!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn {
                        items(rideRequests.sortedWith(compareBy<RideRequest> { it.urgency.ordinal }.thenBy { it.pickupLocation })) { request ->
                            VolunteerRideRequestCard(
                                request = request,
                                onAccept = { 
                                    val mockVolunteer = Volunteer(
                                        name = "You",
                                        vehicleInfo = VehicleInfo(
                                            make = "Toyota",
                                            model = "Camry",
                                            color = "Silver",
                                            licensePlate = "VOL-001"
                                        ),
                                        rating = 4.8f
                                    )
                                    currentRide = RideStatus(
                                        request = request,
                                        volunteer = mockVolunteer,
                                        eta = "5 minutes",
                                        status = RideStatusType.ASSIGNED
                                    )
                                    rideRequests = rideRequests - request
                                }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Turn on availability to see ride requests and help your community!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VolunteerRideRequestCard(
    request: RideRequest,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with urgency and rider info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.urgency.displayName,
                    fontWeight = FontWeight.Bold,
                    color = when (request.urgency) {
                        UrgencyLevel.EMERGENCY -> androidx.compose.ui.graphics.Color.Red
                        UrgencyLevel.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                        UrgencyLevel.MEDIUM -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                        UrgencyLevel.LOW -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    }
                )
                Text(
                    text = "Rider: John Doe", // Mock rider name
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pickup location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pickup",
                    tint = androidx.compose.ui.graphics.Color.Green
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pickup: ${request.pickupLocation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Drop-off location
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Dropoff",
                    tint = androidx.compose.ui.graphics.Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Drop-off: ${request.dropoffLocation}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (request.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${request.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Accept Ride")
            }
        }
    }
}

@Composable
fun VolunteerRideStatusScreen(
    rideStatus: RideStatus,
    onBack: () -> Unit,
    onStatusUpdate: (RideStatusType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top App Bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Your Ride",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = when (rideStatus.status) {
                        RideStatusType.ASSIGNED -> "Ride Accepted"
                        RideStatusType.ON_THE_WAY -> "On the way to pickup"
                        RideStatusType.ARRIVED -> "Arrived at pickup location"
                        RideStatusType.IN_PROGRESS -> "Ride in progress"
                        RideStatusType.COMPLETED -> "Ride completed"
                        else -> "Unknown status"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Route Info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Pickup",
                        tint = androidx.compose.ui.graphics.Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pickup: ${rideStatus.request.pickupLocation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Dropoff",
                        tint = androidx.compose.ui.graphics.Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drop-off: ${rideStatus.request.dropoffLocation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (rideStatus.request.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${rideStatus.request.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
        }
        
        // Action Buttons based on status
        when (rideStatus.status) {
            RideStatusType.ASSIGNED -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.ON_THE_WAY) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Start Driving to Pickup")
                }
            }
            RideStatusType.ON_THE_WAY -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.ARRIVED) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Mark as Arrived")
                }
            }
            RideStatusType.ARRIVED -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.IN_PROGRESS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Start Ride")
                }
            }
            RideStatusType.IN_PROGRESS -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.COMPLETED) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Complete Ride")
                }
            }
            RideStatusType.COMPLETED -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✅ Ride completed successfully! Thank you for helping your community!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    )
                }
            }
            else -> {}
        }
        
        // Cancel button (if not completed)
        if (rideStatus.status != RideStatusType.COMPLETED) {
            TextButton(
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Cancel Ride")
            }
        }
    }
}

@Composable
fun LiftTogetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF6200EE),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6)
        ),
        content = content
    )
}