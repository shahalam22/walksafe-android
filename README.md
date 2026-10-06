# WalkSafe for Android

The native Android app for [WalkSafe](https://github.com/shahalam22/WalkSafe), a navigation
assistant for blind and low-vision pedestrians. It does everything the WalkSafe web app does,
and **keeps guiding with the screen off**.

**Download:** [walksafe.apk](https://github.com/shahalam22/walksafe-android/releases/latest/download/walksafe.apk)
(open it on the phone; allow installing from that source).

## What it does

- **Blind user:** tap anywhere to start; every instruction is spoken (stop, walk forward,
  step left, …). Tap again, shake the phone, or use the notification's Stop button to stop.
  The screen can be turned off while guiding.
- **Admin:** session dashboard (stats, charts, speed profile, CSV export to Downloads),
  accounts (add a user or another admin; new password, turn on/off, delete for users), the
  server address, and phone setup steps. Admins can also open the guidance screen.
- **Forgot password:** after a wrong sign-in, **Forgot password?** emails a reset link. Opening
  it on the phone opens WalkSafe at a **New password** screen; after saving, sign in again.
  The link is `walksafe://reset-password`, which must be in Supabase's allowed Redirect URLs
  (see the main repo's README, "Supabase (once)").

It uses the same backend as the web app: the WalkSafe server on Colab for guidance and
user management, and Supabase for sign-in, session data and the server address.

## How it works

```
GuidanceService (foreground service, camera type, partial wake lock)
  CameraX ImageAnalysis (no preview, so it runs with the screen off)
    → JPEG 518 px → POST /api/frame on the WalkSafe server
    ← command + phrase → TextToSpeech (navigation-guidance audio)
  ShakeDetector (accelerometer) → stop
```

The screens observe the service through `GuidanceStateHolder`; the service, the view models
and the repositories share singletons provided by Hilt.

## Project structure

```
app/src/main/java/io/github/shahalam22/walksafe/
  WalkSafeApp.kt, MainActivity.kt
  di/            Hilt module: Supabase client, HTTP client, DataStore
  data/
    auth/        AuthRepository (Supabase Auth)
    server/      WalkSafeApi (Ktor), ServerConfigRepository, ApiException
    admin/       AdminRepository (Postgrest), CSV export
    prefs/       UserPrefsRepository (DataStore)
    model/       Serializable models
  guidance/      GuidanceService, FrameCapture, Speaker, ShakeDetector, state
  ui/
    login/, guide/, admin/{sessions,users,server,setup}/   screens + view models
    components/, theme/, util/
```

**Stack:** Kotlin, Jetpack Compose (Material 3), MVVM with `ViewModel` + `StateFlow`, Hilt,
Navigation Compose (type-safe routes), Coroutines, CameraX, DataStore, supabase-kt, Ktor.

## Building

Every push to `main` builds a signed APK with GitHub Actions and publishes it as a release
(the download link above always points to the newest). Pull requests build and test only.

Signing uses four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. Keep the same key: Android installs an update only if it is signed with the
key of the installed version. Without the secrets, builds are signed with a debug key.

Locally (JDK 17 and the Android SDK needed):

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

## Phone setup

1. Install the APK, open WalkSafe and sign in with the account the admin created.
2. Helper menu → **Allow camera** (camera and notifications), **Battery: allow background**
   (choose Allow), **Test voice**, **Check server**.
3. Some phones (Xiaomi, Samsung, …) also need Settings → Apps → WalkSafe → Battery →
   **Unrestricted**, or they stop the app with the screen off.
