package com.lifttogether.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.lifttogether.RideRequest
import com.lifttogether.Volunteer
import com.lifttogether.VehicleInfo
import com.lifttogether.RideStatus
import com.lifttogether.UrgencyLevel
import com.lifttogether.RideStatusType

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    // Authentication
    suspend fun signUp(email: String, password: String, userType: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Save user type to Firestore
                firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "email" to email,
                        "userType" to userType,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user.uid)
            } else {
                Result.failure(Exception("Failed to sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun getCurrentUser() = auth.currentUser
    
    // Ride Requests
    suspend fun createRideRequest(rideRequest: RideRequest): Result<String> {
        return try {
            val rideRequestMap = mapOf(
                "pickupLocation" to rideRequest.pickupLocation,
                "dropoffLocation" to rideRequest.dropoffLocation,
                "urgency" to rideRequest.urgency.name,
                "notes" to rideRequest.notes,
                "status" to "PENDING",
                "createdAt" to System.currentTimeMillis()
            )
            val docRef = firestore.collection("rideRequests").add(rideRequestMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRideRequests(): Result<List<RideRequest>> {
        return try {
            val snapshot = firestore.collection("rideRequests")
                .whereEqualTo("status", "PENDING")
                .get().await()
            val requests = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    RideRequest(
                        pickupLocation = data["pickupLocation"] as? String ?: "",
                        dropoffLocation = data["dropoffLocation"] as? String ?: "",
                        urgency = UrgencyLevel.valueOf(data["urgency"] as? String ?: "MEDIUM"),
                        notes = data["notes"] as? String ?: "",
                        id = doc.id
                    )
                } else null
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateRideRequestStatus(requestId: String, status: RideStatusType): Result<Unit> {
        return try {
            firestore.collection("rideRequests").document(requestId)
                .update("status", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Volunteers
    suspend fun updateVolunteerAvailability(volunteerId: String, isAvailable: Boolean): Result<Unit> {
        return try {
            firestore.collection("volunteers").document(volunteerId)
                .update("isAvailable", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableVolunteers(): Result<List<Volunteer>> {
        return try {
            val snapshot = firestore.collection("volunteers")
                .whereEqualTo("isAvailable", true)
                .get().await()
            val volunteers = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    val vehicleInfo = data["vehicleInfo"] as? Map<String, Any>
                    Volunteer(
                        name = data["name"] as? String ?: "Unknown",
                        vehicleInfo = if (vehicleInfo != null) {
                            VehicleInfo(
                                make = vehicleInfo["make"] as? String ?: "",
                                model = vehicleInfo["model"] as? String ?: "",
                                color = vehicleInfo["color"] as? String ?: "",
                                licensePlate = vehicleInfo["licensePlate"] as? String ?: ""
                            )
                        } else {
                            VehicleInfo("Unknown", "Unknown", "Unknown", "Unknown")
                        },
                        rating = (data["rating"] as? Double)?.toFloat() ?: 4.5f,
                        isAvailable = data["isAvailable"] as? Boolean ?: false,
                        id = doc.id
                    )
                } else null
            }
            Result.success(volunteers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time updates
    fun listenToRideRequests(callback: (List<RideRequest>) -> Unit) {
        firestore.collection("rideRequests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        RideRequest(
                            pickupLocation = data["pickupLocation"] as? String ?: "",
                            dropoffLocation = data["dropoffLocation"] as? String ?: "",
                            urgency = UrgencyLevel.valueOf(data["urgency"] as? String ?: "MEDIUM"),
                            notes = data["notes"] as? String ?: "",
                            id = doc.id
                        )
                    } else null
                } ?: emptyList()
                callback(requests)
            }
    }
}
