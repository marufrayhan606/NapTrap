# Firebase Configuration Setup

## Security Notice
This project uses secure configuration practices to protect API keys and sensitive data.

## Setup Instructions

### 1. Local Properties Configuration
1. Copy `local.properties.template` to `local.properties`
2. Fill in your actual Firebase Web Client ID in `local.properties`
3. Make sure `local.properties` is in `.gitignore` (it should be by default)

### 2. Firebase Console Setup
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Your Apps**
4. Copy the **Web Client ID** from your Android app configuration
5. Add this to your `local.properties` file

### 3. Google Services JSON
1. Download `google-services.json` from Firebase Console
2. Place it in the `app/` directory
3. This file is gitignored for security

### 4. Enable Authentication
In Firebase Console:
1. Go to **Authentication** → **Sign-in method**
2. Enable **Email/Password**
3. Enable **Google** sign-in
4. Add your app's SHA-1 fingerprint

## Files Protected from Version Control
- `local.properties` - Contains API keys
- `google-services.json` - Contains Firebase configuration
- `*.properties` files - May contain sensitive data

## BuildConfig Usage
The app uses Android's BuildConfig to securely access API keys at runtime without hardcoding them in source code.
