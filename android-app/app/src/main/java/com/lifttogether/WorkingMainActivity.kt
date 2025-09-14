package com.lifttogether

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ExitToApp
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
    EMERGENCY("Emergency"), HIGH("High"), MEDIUM("Medium"), LOW("Low")
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
    PENDING, ASSIGNED, ON_THE_WAY, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
}

// Mock user database for fallback authentication
data class MockUser(
    val email: String,
    val password: String,
    val userType: String
)

val mockUsers = listOf(
    MockUser("manoj@gmail.com", "password123", "Rider"),
    MockUser("user1@gmail.com", "password123", "Volunteer"),
    MockUser("rider@gmail.com", "password123", "Rider"),
    MockUser("volunteer@gmail.com", "password123", "Volunteer"),
    MockUser("test@gmail.com", "password123", "Rider")
)

// Check if Firebase is available and working
suspend fun isFirebaseAvailable(): Boolean {
    return try {
        val auth = FirebaseAuth.getInstance()
        // Try to get current user to test Firebase connection
        auth.currentUser
        true
    } catch (e: Exception) {
        false
    }
}

// Mock authentication functions
fun authenticateMockUser(email: String, password: String): MockUser? {
    return mockUsers.find { it.email == email && it.password == password }
}

class WorkingMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Log warning but don't crash
            android.util.Log.w("WorkingMainActivity", "Firebase initialization failed: ${e.message}")
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
    }
}

@Composable
fun LiftTogetherApp() {
    var userType by remember { mutableStateOf("") }
    var isSignedIn by remember { mutableStateOf(false) }
    var showAuth by remember { mutableStateOf(false) }
    var useFirebase by remember { mutableStateOf(true) }

    // Check Firebase Auth state with fallback
    LaunchedEffect(Unit) {
        val firebaseAvailable = isFirebaseAvailable()
        useFirebase = firebaseAvailable
        
        if (firebaseAvailable) {
            try {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                
                if (currentUser != null) {
                    isSignedIn = true
                    // For demo purposes, we'll use mock user types
                    // In a real app, you'd fetch this from Firestore
                    userType = if (currentUser.email?.contains("rider") == true) "Rider" else "Volunteer"
                } else {
                    isSignedIn = false
                    userType = ""
                }
            } catch (e: Exception) {
                // Firebase failed, fall back to mock auth
                useFirebase = false
                isSignedIn = false
                userType = ""
            }
        } else {
            // Firebase not available, use mock auth
            useFirebase = false
            isSignedIn = false
            userType = ""
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
                },
                useFirebase = useFirebase
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
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
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

        Text(
            text = "Please sign in to continue:",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Already have an account?",
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
    onSignIn: (String) -> Unit,
    useFirebase: Boolean = true
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

        // Authentication Mode Indicator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (useFirebase) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else 
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (useFirebase) Icons.Default.Star else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (useFirebase) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (useFirebase) 
                        "Using Firebase Authentication" 
                    else 
                        "Using Mock Authentication (Firebase unavailable)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (useFirebase) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            }
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
                                if (useFirebase) {
                                    try {
                                        val auth = FirebaseAuth.getInstance()
                                        if (isSignUp) {
                                            // Sign up with Firebase
                                            auth.createUserWithEmailAndPassword(emailPhone, password).await()
                                        } else {
                                            // Sign in with Firebase
                                            auth.signInWithEmailAndPassword(emailPhone, password).await()
                                        }
                                        isLoading = false
                                        onSignIn(userType)
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = e.message ?: "Authentication failed"
                                    }
                                } else {
                                    // Use mock authentication
                                    delay(1000) // Simulate network delay
                                    
                                    if (isSignUp) {
                                        // For sign up, just check if user doesn't exist
                                        val existingUser = authenticateMockUser(emailPhone, password)
                                        if (existingUser != null) {
                                            errorMessage = "User already exists"
                                        } else {
                                            // Add new user to mock database (in real app, this would be stored)
                                            isLoading = false
                                            onSignIn(userType)
                                        }
                                    } else {
                                        // Sign in with mock authentication
                                        val mockUser = authenticateMockUser(emailPhone, password)
                                        if (mockUser != null) {
                                            isLoading = false
                                            onSignIn(mockUser.userType)
                                        } else {
                                            errorMessage = "Invalid email or password"
                                        }
                                    }
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
                
                // Mock user help text
                if (!useFirebase) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Mock Users Available:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• manoj@gmail.com (Rider)\n• user1@gmail.com (Volunteer)\n• rider@gmail.com (Rider)\n• volunteer@gmail.com (Volunteer)\n• test@gmail.com (Rider)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Password for all: password123",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
            Text(
                text = "Welcome, $userType!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.primary
                )
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
                val cancelledRideId = currentRideStatus?.request?.id
                currentRideStatus = null
                // Remove the cancelled ride from the list
                rideRequests = rideRequests.filter { it.id != cancelledRideId }
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
                onClick = { 
                    if (currentRideStatus == null) {
                        showRequestForm = true
                    }
                    // If there's an active ride, do nothing (button is disabled)
                },
                enabled = currentRideStatus == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentRideStatus == null) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    if (currentRideStatus == null) "Request New Ride" else "Ride in Progress",
                    color = if (currentRideStatus == null) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
            
            // Show helpful message when button is disabled
            if (currentRideStatus != null) {
                Text(
                    text = "You have an active ride. Cancel it to request a new one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Ride request form
            if (showRequestForm) {
                RequestRideDialog(
                    onDismiss = { showRequestForm = false },
                    onRequestSubmitted = { request ->
                        // Only allow one active ride at a time
                        if (currentRideStatus == null) {
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
                        } else {
                            // Show a message that there's already an active ride
                            showRequestForm = false
                            // Note: In a real app, you'd show a proper dialog or snackbar
                            // For now, we'll just close the form
                        }
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
    var showCancelDialog by remember { mutableStateOf(false) }
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
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Red
                )
            ) {
                Text("Cancel Ride")
            }
        }
        
        // Cancel Confirmation Dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Ride") },
                text = { Text("Are you sure you want to cancel this ride?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            onCancel()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Red
                        )
                    ) {
                        Text("Yes, Cancel")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Ride")
                    }
                }
            )
        }
    }
}

@Composable
fun VolunteerScreen() {
    var isAvailable by remember { mutableStateOf(false) }
    var currentRideStatus by remember { mutableStateOf<RideStatus?>(null) }
    
    // Mock ride requests
    val mockRideRequests = remember {
        listOf(
            RideRequest(
                pickupLocation = "123 Main St, Downtown",
                dropoffLocation = "456 Oak Ave, Uptown",
                urgency = UrgencyLevel.EMERGENCY,
                notes = "Wheelchair accessible vehicle needed"
            ),
            RideRequest(
                pickupLocation = "789 Pine St, Midtown",
                dropoffLocation = "321 Elm St, Eastside",
                urgency = UrgencyLevel.HIGH,
                notes = "Medical appointment"
            ),
            RideRequest(
                pickupLocation = "555 Cedar Blvd, Westside",
                dropoffLocation = "777 Maple Dr, Northside",
                urgency = UrgencyLevel.MEDIUM,
                notes = ""
            )
        )
    }

    if (currentRideStatus != null) {
        VolunteerRideStatusScreen(
            rideStatus = currentRideStatus!!,
            onBack = { currentRideStatus = null },
            onStatusUpdate = { newStatus ->
                currentRideStatus = currentRideStatus?.copy(status = newStatus)
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
                            text = "Availability",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAvailable) "Available to give rides" else "Not available",
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

                LazyColumn {
                    items(mockRideRequests) { request ->
                        VolunteerRideRequestCard(
                            request = request,
                            onAccept = {
                                val mockVolunteer = Volunteer(
                                    name = "You",
                                    vehicleInfo = VehicleInfo(
                                        make = "Honda",
                                        model = "Civic",
                                        color = "Blue",
                                        licensePlate = "VOL-001"
                                    ),
                                    rating = 4.8f
                                )
                                currentRideStatus = RideStatus(
                                    request = request,
                                    volunteer = mockVolunteer,
                                    eta = "5 minutes",
                                    status = RideStatusType.ON_THE_WAY
                                )
                            }
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Turn on availability to see ride requests",
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
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Accept Ride")
                }
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
                text = "Ride in Progress",
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
                        RideStatusType.ON_THE_WAY -> "On the way to pickup"
                        RideStatusType.ARRIVED -> "Arrived at pickup location"
                        RideStatusType.IN_PROGRESS -> "Ride in progress"
                        RideStatusType.COMPLETED -> "Ride completed"
                        else -> "Ride status"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "ETA: ${rideStatus.eta}",
                    fontSize = 16.sp,
                    color = androidx.compose.ui.graphics.Color(0xFF2196F3)
                )
            }
        }

        // Action Buttons
        when (rideStatus.status) {
            RideStatusType.ON_THE_WAY -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.ARRIVED) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Arrived")
                }
            }
            RideStatusType.ARRIVED -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.IN_PROGRESS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Ride")
                }
            }
            RideStatusType.IN_PROGRESS -> {
                Button(
                    onClick = { onStatusUpdate(RideStatusType.COMPLETED) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Ride")
                }
            }
            RideStatusType.COMPLETED -> {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ride completed successfully!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route Details
        Card(
            modifier = Modifier.fillMaxWidth()
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
    }
}

@Composable
fun LiftTogetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
