# 🚀 How to Run LiftTogether in Android Studio

## Prerequisites

Before running the app, you need to set up a few things:

### 1. **Android Studio Setup**
- Download and install [Android Studio](https://developer.android.com/studio) (Arctic Fox or later)
- Install Android SDK 24+ (API level 24 or higher)
- Set up an Android Virtual Device (AVD) or connect a physical device

### 2. **Firebase Project Setup** (Required)
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a new project called "LiftTogether"
3. Enable the following services:
   - **Authentication** → Sign-in method → Email/Password
   - **Firestore Database** → Create database in test mode
   - **Realtime Database** → Create database
   - **Cloud Messaging** → Enable
4. Download `google-services.json` and replace the existing one in `android-app/app/`

### 3. **Google Maps API Key** (Required)
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing one
3. Enable "Maps SDK for Android"
4. Create an API key
5. Update the key in `android-app/app/src/main/res/values/strings.xml`

## Step-by-Step Setup

### **Step 1: Open Project in Android Studio**

1. **Launch Android Studio**
2. **Open Project**: Click "Open an existing Android Studio project"
3. **Navigate to**: `/Users/pjain/workspace/app/LiftTogether/android-app`
4. **Select**: The `android-app` folder (not the root LiftTogether folder)
5. **Click**: "OK"

### **Step 2: Configure Firebase**

1. **Replace google-services.json**:
   ```
   Copy your downloaded google-services.json to:
   android-app/app/google-services.json
   ```

2. **Update Google Maps API Key**:
   - Open `android-app/app/src/main/res/values/strings.xml`
   - Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual API key

### **Step 3: Sync Project**

1. **Gradle Sync**: Android Studio will automatically prompt to sync
2. **Click**: "Sync Now" or go to File → Sync Project with Gradle Files
3. **Wait**: For dependencies to download (first time may take 5-10 minutes)

### **Step 4: Set Up Device/Emulator**

#### **Option A: Physical Android Device (Recommended for GPS testing)**
1. **Enable Developer Options** on your Android device
2. **Enable USB Debugging**
3. **Connect via USB** to your computer
4. **Allow USB Debugging** when prompted on device
5. **Select device** in Android Studio's device dropdown

#### **Option B: Android Emulator**
1. **Open AVD Manager**: Tools → AVD Manager
2. **Create Virtual Device**: Click "Create Virtual Device"
3. **Choose Device**: Select a phone (e.g., Pixel 6)
4. **Select System Image**: Choose API 24+ (Android 7.0+)
5. **Configure AVD**: Give it a name and click "Finish"
6. **Start Emulator**: Click the play button next to your AVD

### **Step 5: Build and Run**

1. **Select Target Device**: Choose your device/emulator from dropdown
2. **Build Project**: Build → Make Project (Ctrl+F9 / Cmd+F9)
3. **Run App**: Click the green "Run" button (Shift+F10 / Ctrl+R)
4. **Wait**: For the app to install and launch

## 🎯 **Testing the App**

### **First Launch**
1. **Splash Screen**: App will show LiftTogether logo for 2 seconds
2. **Login Screen**: You'll see the authentication screen
3. **User Type Selection**: Choose "Rider" or "Volunteer"

### **Test as Rider**
1. **Sign Up**: Create a new rider account
2. **Request Ride**: Tap the + button to request a ride
3. **Set Location**: Allow location permissions
4. **Select Urgency**: Choose Emergency, High, Medium, or Low
5. **Submit Request**: Your ride request will be created

### **Test as Volunteer**
1. **Sign Up**: Create a new volunteer account
2. **Toggle Availability**: Turn on "Available to give rides"
3. **View Requests**: See available ride requests
4. **Accept Ride**: Tap "Accept" on a ride request
5. **Update Status**: Mark as "On the way", "Arrived", "Completed"

## 🔧 **Troubleshooting**

### **Common Issues & Solutions**

#### **1. Build Errors**
```
Error: Could not find google-services.json
```
**Solution**: Make sure you've downloaded and placed the correct `google-services.json` file

#### **2. Maps Not Loading**
```
Error: Google Maps API key not found
```
**Solution**: Update the API key in `strings.xml` and rebuild

#### **3. Location Permission Denied**
```
Error: Location permission not granted
```
**Solution**: 
- Go to Settings → Apps → LiftTogether → Permissions
- Enable Location permission
- Or test on emulator with location simulation

#### **4. Firebase Connection Issues**
```
Error: Firebase connection failed
```
**Solution**: 
- Check internet connection
- Verify Firebase project configuration
- Ensure all Firebase services are enabled

#### **5. Gradle Sync Failed**
```
Error: Gradle sync failed
```
**Solution**:
- Check internet connection
- Go to File → Invalidate Caches and Restart
- Try: Build → Clean Project, then Build → Rebuild Project

### **Debug Mode**
- **Enable Logcat**: View → Tool Windows → Logcat
- **Filter by Tag**: Search for "LiftTogether" to see app logs
- **Check Errors**: Look for red error messages

## 📱 **Testing Features**

### **Location Testing**
- **Real Device**: Best for GPS testing
- **Emulator**: Use Extended Controls → Location to simulate GPS

### **Firebase Testing**
- **Firestore**: Check data in Firebase Console → Firestore
- **Auth**: View users in Firebase Console → Authentication
- **Functions**: Monitor in Firebase Console → Functions

### **Push Notifications**
- **Test**: Accept a ride request to trigger notifications
- **Check**: Firebase Console → Cloud Messaging

## 🚀 **Ready to Go!**

Once everything is set up, you should see:
- ✅ App launches without errors
- ✅ Login/Signup screens work
- ✅ Location permissions are granted
- ✅ Maps load correctly
- ✅ Firebase data syncs properly

The app is now ready for development and testing! 🎉

## 📞 **Need Help?**

If you encounter any issues:
1. Check the troubleshooting section above
2. Look at the Logcat for error messages
3. Verify all prerequisites are installed
4. Ensure Firebase and Google Maps are properly configured

Happy coding! 🚀
