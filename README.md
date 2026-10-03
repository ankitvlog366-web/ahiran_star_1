# My App (Android Application)

- **Application ID**: `com.ankit.app`
- **App Name**: `My App`
- **Platform**: Android (Android Studio / Gradle compatible & Capacitor compatible)
- **Min SDK**: 24 (Android 7.0+)
- **Target SDK**: 36

---

## Features
- **Portrait Orientation**: Locked to portrait orientation for standard smartphone use.
- **Edge-to-Edge Layout**: Fully supports modern edge-to-edge layouts with window insets.
- **Back Button Navigation**: Handles back navigation stack gracefully, exiting cleanly at root.
- **Network Permissions**: Configured for `INTERNET` and `ACCESS_NETWORK_STATE` with offline/error fallbacks.
- **Media & Camera**: Photo Picker (`PickVisualMedia`) and Camera integration for image capture, crop, rotate, and zoom.
- **External Links**: External URLs open in the system default browser via Android Intents.
- **Splash Screen**: Configured in `themes.xml` with adaptive launcher icons.

---

## Build Instructions

### 1. Opening in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose the root folder of this project.
3. Allow Gradle to sync.
4. Select your connected Android device or emulator.
5. Click **Run** (`Shift + F10`) to build and run the app.

---

### 2. Generating a Debug APK via Terminal

On Linux / macOS:
```bash
./gradlew assembleDebug
```

On Windows:
```cmd
gradlew.bat assembleDebug
```

The generated APK will be available at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

### 3. Generating a Signed Release APK / Bundle

1. In Android Studio, go to **Build > Generate Signed Bundle / APK...**
2. Choose **APK** or **Android App Bundle (AAB)**.
3. Select your keystore file and enter the credentials.
4. Select `release` build variant and click **Finish**.

Alternatively, via command line with environment variables configured:
```bash
KEYSTORE_PATH="/path/to/my-upload-key.jks" \
STORE_PASSWORD="your-store-password" \
KEY_PASSWORD="your-key-password" \
./gradlew assembleRelease
```
The release APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

---

### 4. Capacitor / NPM Workflow (Optional Web Sync)

If synchronizing with Capacitor web assets:
```bash
# 1. Install dependencies
npm install

# 2. Build assets
npm run build

# 3. Sync with Android project
npx cap sync android

# 4. Open in Android Studio
npx cap open android
```
