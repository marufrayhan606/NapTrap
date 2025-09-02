# Firebase Authentication Setup for NapTrap

## 🔐 Authentication Features Implemented

### ✅ **Authentication Methods**
- **Email/Password**: Traditional sign up and sign in
- **Google Sign-In**: One-tap Google authentication using Credentials API
- **Automatic State Management**: Persistent authentication across app restarts

### ✅ **Screens Added**
1. **LoginScreen** - Modern email/password + Google sign-in
2. **SignUpScreen** - Account creation with validation
3. **ProfileScreen** - User info display and sign out
4. **Protected Routes** - Authentication-gated navigation

### ✅ **Key Components**
- **AuthViewModel** - Centralized authentication state management
- **GoogleSignInHelper** - Modern Credentials API implementation
- **Navigation Flow** - Automatic routing based on auth state

## 🚀 **Setup Instructions**

### 1. Firebase Console Configuration
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your NapTrap project
3. Enable **Authentication** → **Sign-in Methods**
4. Enable:
   - **Email/Password**
   - **Google** (add your SHA-1 fingerprint)

### 2. Update Google Sign-In Configuration
In `GoogleSignInHelper.kt`, replace:
```kotlin
.setServerClientId("YOUR_WEB_CLIENT_ID") // Replace with your actual web client ID
```

**To find your Web Client ID:**
1. Firebase Console → Project Settings → General
2. Your apps → Web app → Web client ID
3. Copy the client ID (ends with `.googleusercontent.com`)

### 3. SHA-1 Fingerprint (for Google Sign-In)
Run this command to get your debug SHA-1:
```bash
./gradlew signingReport
```
Add the SHA-1 to Firebase Console → Project Settings → Your apps → Add fingerprint

## 📱 **User Flow**

### **Unauthenticated Users**
1. **App Launch** → LoginScreen
2. **New User** → SignUpScreen → Email verification → HomeScreen
3. **Existing User** → LoginScreen → HomeScreen
4. **Google User** → One-tap sign in → HomeScreen

### **Authenticated Users**
1. **App Launch** → HomeScreen (automatic)
2. **Profile Access** → Profile button in header → ProfileScreen
3. **Sign Out** → ProfileScreen → Returns to LoginScreen

## 🎨 **UI/UX Features**

### **Modern Design**
- Consistent orange gradient theme
- Light/Dark mode support
- Material 3 design language
- Smooth animations and transitions

### **User Experience**
- **Loading States** - Visual feedback during authentication
- **Error Handling** - Clear error messages
- **Form Validation** - Real-time input validation
- **Password Toggle** - Show/hide password functionality

### **Security**
- **Password Requirements** - Minimum 6 characters
- **Input Validation** - Email format and password confirmation
- **Secure Storage** - Firebase handles token management

## 📂 **File Structure**
```
auth/
├── AuthViewModel.kt          # Authentication state management
└── GoogleSignInHelper.kt     # Google Sign-In implementation

screens/
├── LoginScreen.kt           # Email/password + Google sign-in
├── SignUpScreen.kt          # Account creation
├── ProfileScreen.kt         # User profile and sign out
└── HomeScreen.kt            # Updated with profile button

navigation/
└── NavGraph.kt              # Updated with auth routing
```

## 🔧 **Dependencies Added**
Already included in your `build.gradle.kts`:
- `firebase-auth` - Firebase Authentication
- `androidx.credentials` - Modern authentication APIs
- `androidx.credentials.play-services-auth` - Google Play Services integration
- `googleid` - Google ID token handling

## 🎯 **Next Steps**
1. **Test Authentication** - Run the app and test sign up/sign in flows
2. **Update Web Client ID** - Replace placeholder in GoogleSignInHelper
3. **Add SHA-1 Fingerprint** - For Google Sign-In to work
4. **Customize Branding** - Update app name and colors in authentication screens

Your NapTrap app now has a complete, modern authentication system! 🎉
