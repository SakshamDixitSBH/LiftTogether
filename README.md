
![WhatsApp Image 2025-09-11 at 03 39 12 (1)](https://github.com/user-attachments/assets/727db507-247e-4d20-a288-dbfe4d7af0c4)

![WhatsApp Image 2025-09-11 at 03 39 12](https://github.com/user-attachments/assets/7b37d5ab-b04d-4967-a1c6-827b37dbba5e)


# LiftTogether - Android App MVP

A volunteer-driven ride-sharing app that connects riders in need with volunteer drivers, prioritizing requests based on urgency.

## Features

### Rider App
- **Sign Up/Login** - Email/Phone authentication with Firebase
- **Request Ride** - Pickup/dropoff locations with GPS auto-detect
- **Urgency Selection** - Emergency, High, Medium, Low priority levels
- **Ride Tracking** - Real-time status updates and volunteer information
- **Cancel Ride** - Ability to cancel pending rides

### Volunteer App
- **Sign Up/Login** - Same authentication as riders
- **Availability Toggle** - Turn availability on/off
- **View Requests** - List of pending rides sorted by urgency + proximity
- **Accept Rides** - Accept and manage ride requests
- **Status Updates** - On the way → Arrived → Ride completed

## Tech Stack

- **Frontend**: Android (Kotlin) with Jetpack Compose
- **Backend**: Firebase (Auth, Firestore, Realtime DB, Cloud Functions)
- **Maps**: Google Maps API
- **Notifications**: Firebase Cloud Messaging
- **Architecture**: MVVM with Repository pattern

## Project Structure

```
LiftTogether/
├── android-app/                 # Android application
│   ├── app/
│   │   ├── src/main/java/com/lifttogether/
│   │   │   ├── auth/           # Authentication screens
│   │   │   ├── rider/          # Rider-specific features
│   │   │   ├── volunteer/      # Volunteer-specific features
│   │   │   ├── shared/         # Shared components
│   │   │   ├── models/         # Data models
│   │   │   └── services/       # Firebase services
│   │   └── src/main/res/       # Resources
│   └── build.gradle
├── firebase-functions/          # Cloud Functions
│   └── src/
│       └── index.ts
└── docs/                        # Documentation
```

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Firebase project
- Google Maps API key

### 1. Firebase Setup
1. Create a new Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable Authentication (Email/Password)
3. Enable Firestore Database
4. Enable Realtime Database
5. Enable Cloud Messaging
6. Download `google-services.json` and place in `android-app/app/`

### 2. Google Maps Setup
1. Get API key from [Google Cloud Console](https://console.cloud.google.com)
2. Enable Maps SDK for Android
3. Update `google_maps_key` in `strings.xml`

### 3. Android Setup
1. Open `android-app` folder in Android Studio
2. Sync project with Gradle files
3. Build and run on device/emulator

### 4. Firebase Functions Setup
```bash
cd firebase-functions
npm install
npm run build
firebase deploy --only functions
```

## Key Files

### Android App
- `MainActivity.kt` - Main entry point
- `auth/` - Login/Signup screens
- `rider/RiderMainScreen.kt` - Rider interface
- `volunteer/VolunteerMainScreen.kt` - Volunteer interface
- `services/FirebaseRepository.kt` - Firebase data access
- `models/` - Data models (User, RideRequest, Volunteer)

### Firebase Functions
- `src/index.ts` - Cloud Functions for ride matching and notifications

## Data Models

### User
```kotlin
data class User(
    val id: String,
    val email: String,
    val name: String,
    val phone: String,
    val userType: UserType, // RIDER, VOLUNTEER, ADMIN
    val isVerified: Boolean
)
```

### RideRequest
```kotlin
data class RideRequest(
    val id: String,
    val riderId: String,
    val pickupLocation: Location,
    val dropoffLocation: Location,
    val urgency: UrgencyLevel, // EMERGENCY, HIGH, MEDIUM, LOW
    val status: RideStatus, // PENDING, ACCEPTED, ON_THE_WAY, etc.
    val assignedVolunteerId: String
)
```

## Testing

### Android Testing
- **Unit Tests**: `src/test/java/`
- **Integration Tests**: `src/androidTest/java/`
- **Device Testing**: Physical Android device (recommended for GPS)
- **Emulator Testing**: Android emulator with location simulation

### Firebase Testing
- **Firestore**: Use Firebase Console to view data
- **Functions**: Test in Firebase Console Functions tab
- **Notifications**: Test with Firebase Cloud Messaging

## Deployment

### Android App
1. Generate signed APK in Android Studio
2. Upload to Google Play Store
3. Configure Firebase for production

### Firebase Functions
```bash
firebase deploy --only functions
```

## Security Considerations

- Location data encryption
- User verification system
- Rate limiting for ride requests
- Secure Firebase rules
- API key protection

## Future Enhancements

- Admin panel for verification
- Payment integration
- Ride history and ratings
- Push notifications
- Offline support
- Multi-language support

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, email support@lifttogether.com or create an issue in the repository.
