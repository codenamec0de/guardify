# Guardify - Privacy Guard Android App

A comprehensive mobile security application that helps users audit installed apps, monitor permissions, detect background data usage, and identify potential privacy threats.

## Features

### V1.0 — Core Privacy Audit

#### 1. **User Authentication**
- Email/Password login and registration
- Google Sign-In integration
- Secure Firebase Authentication

#### 2. **Onboarding Flow**
- Permission explanation screen
- SMS permission request for scam detection (optional)
- App scanning permission (automatic on Android 11+)

#### 3. **Home Dashboard**
- Quick scan summary with risk counts
- One-tap device scanning
- Quick action buttons

#### 4. **App Audit**
- Real-time scanning of all installed applications
- Permission analysis for each app
- Risk classification (High / Medium / Low)
- Search functionality to find specific apps
- Filter by risk level

#### 5. **App Details**
- View all permissions for any app
- Exodus Privacy API integration for tracker detection
- Direct link to system settings to manage permissions
- One-tap uninstall option

#### 6. **Settings**
- User profile display
- Logout functionality

---

### V1.1 — Background Monitoring & Audit Overhaul

#### 1. **Real Permission Scanning**
- Shows only permissions apps **actually have** (dangerous/user-granted), not auto-granted normal permissions
- Eliminates misleading permission displays that could guide users in the wrong direction
- Uses `requestedPermissionsFlags` + `PROTECTION_DANGEROUS` filtering instead of `checkPermission()`

#### 2. **Accurate Permission Labels**
- Replaced pattern-matching (`contains()`) with exact lookup maps for all 40+ Android permissions
- Eliminates duplicate labels (e.g., Storage no longer appears twice)
- Every permission has a unique, human-readable name and description

#### 3. **Background Usage Monitoring**
- Detects apps that access permissions and transmit data while running in the background
- Tracks how long each app ran in the background and how much data it used
- Generates alert messages: *"Instagram used its Microphone access while in the background for 5 min, using 2.3 MB of data"*
- Energy-efficient 15-minute periodic scanning via WorkManager (respects Doze mode)
- Uses `UsageStatsManager` for background state detection and `NetworkStatsManager` for per-app data usage

#### 4. **Alerts Page**
- Real-time alert feed showing background permission usage events
- Unread alert count with visual indicators
- Tap any alert to view the app's full permission details
- Clear all alerts with confirmation dialog
- Built-in test button to verify the detection pipeline works end-to-end

#### 5. **Background Detection Test**
- One-tap test button on the Alerts page
- Downloads 1–10 MB of data over ~20 seconds via a foreground service
- Proves the data usage detection pipeline works: minimize the app, wait for the download to finish, and see the alert appear
- Handles all permission flows (Usage Stats, Notification permission on Android 13+)

#### 6. **Audit Category View**
- Toggle between **"By App"** and **"By Category"** views
- Groups related permissions together (Camera + Video + Photo, Precise + Approximate Location, etc.)
- Expandable permission groups showing which apps use each category
- 9 distinct permission categories with custom icons

---

## Setup Instructions

### Prerequisites
- Android Studio (Arctic Fox or newer)
- macOS, Windows, or Linux
- JDK 11 or newer

### Step 1: Open in Android Studio
1. Open Android Studio
2. Select **File > Open**
3. Navigate to the `Guardify` folder and open it
4. Wait for Gradle sync to complete

### Step 2: Add your own google-services.json
**IMPORTANT:** You must provide your own `app/google-services.json` from Firebase Console. This file is excluded from the repository for security reasons.

Your Firebase project must have:
- Package name: `com.uow.guardify`
- Email/Password authentication enabled
- Google Sign-In enabled (with SHA-1 fingerprint added)

### Step 3: Add SHA-1 for Google Sign-In
Run this command in Terminal:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 fingerprint and add it to:
**Firebase Console > Project Settings > Your App > Add Fingerprint**

### Step 4: Build and Run
1. Connect an Android device or start an emulator (API 26+)
2. Click **Run**
3. Grant permissions when prompted (Usage Stats access is required for background monitoring)

---

## Project Structure

```
app/src/main/
├── java/com/uow/guardify/
│   ├── adapter/
│   │   ├── AlertAdapter.kt            # Alert list with unread indicators
│   │   ├── AppListAdapter.kt          # RecyclerView adapter for app list
│   │   ├── PermissionAdapter.kt       # Adapter for permission list
│   │   ├── PermissionGroupAdapter.kt  # Expandable category groups
│   │   └── TrackerAdapter.kt          # Adapter for tracker list
│   ├── api/
│   │   ├── ExodusApiService.kt        # Exodus Privacy API interface
│   │   └── RetrofitClient.kt          # Retrofit singleton
│   ├── model/
│   │   ├── AppInfo.kt                 # App data model with risk calculation
│   │   ├── PermissionAlert.kt         # Background usage alert model
│   │   ├── PermissionGroup.kt         # Category grouping model
│   │   └── TrackerInfo.kt             # Tracker data models
│   ├── service/
│   │   └── TestDataUsageService.kt    # Foreground service for detection test
│   ├── ui/
│   │   ├── home/HomeFragment.kt       # Home dashboard
│   │   ├── audit/AuditFragment.kt     # App audit with By App / By Category toggle
│   │   ├── alerts/AlertsFragment.kt   # Background usage alerts
│   │   └── settings/SettingsFragment.kt
│   ├── util/
│   │   ├── AlertStorage.kt            # JSON-based alert persistence
│   │   ├── AppScanner.kt              # Scans installed apps (dangerous-only)
│   │   ├── BackgroundUsageMonitor.kt  # Core background detection engine
│   │   ├── DataUsageHelper.kt         # Usage Stats permission helper
│   │   ├── PermissionHelper.kt        # Permission metadata (exact lookup maps)
│   │   ├── PermissionMonitorWorker.kt # WorkManager periodic scanner
│   │   ├── PreferencesManager.kt      # SharedPreferences manager
│   │   └── TrackerRepository.kt       # Exodus API handler
│   ├── LoginActivity.kt
│   ├── OnboardingActivity.kt
│   ├── SplashActivity.kt
│   ├── MainActivity.kt
│   ├── AppDetailActivity.kt
│   └── ScanActivity.kt
└── res/
    ├── layout/
    ├── drawable/
    ├── values/
    └── menu/
```

---

## Permissions Used

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Firebase auth & Exodus API |
| `ACCESS_NETWORK_STATE` | Check connectivity |
| `QUERY_ALL_PACKAGES` | List all installed apps |
| `PACKAGE_USAGE_STATS` | Detect background app activity & data usage |
| `FOREGROUND_SERVICE` | Run background detection test service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Android 14+ foreground service type |
| `POST_NOTIFICATIONS` | Show test service progress (Android 13+) |
| `READ_SMS` (optional) | Scan messages for scam links |
| `RECEIVE_SMS` (optional) | Monitor incoming messages |

---

## Risk Classification Logic

```kotlin
High Risk = (sensitivePermissions >= 3 && hasInternet && hasHighRiskPermission)
Medium Risk = (sensitivePermissions >= 2 && hasInternet) || sensitivePermissions >= 1
Low Risk = Everything else

// High Risk Permissions:
- READ_SMS, SEND_SMS
- READ_CALL_LOG
- ACCESS_BACKGROUND_LOCATION
- RECORD_AUDIO
- CAMERA
```

---

## Exodus Privacy API

The app uses the [Exodus Privacy API](https://reports.exodus-privacy.eu.org/api/) to detect trackers in apps.

**Endpoints used:**
- `GET /api/search/{package_name}/details` — Get tracker report for an app
- `GET /api/trackers` — Get all known trackers

---

## Design

Dark theme design:
- Background: `#101922`
- Cards: `#161E2C`
- Primary: `#2B8CEE`
- High Risk: `#EF4444`
- Medium Risk: `#F59E0B`
- Low Risk: `#10B981`

---

## Known Limitations

1. **Cannot revoke permissions programmatically** — Android security model requires user to manually manage permissions in Settings
2. **Tracker detection requires internet** — Exodus API must be reachable
3. **SMS scanning** — Requires explicit user permission and may trigger Play Store review
4. **Usage Stats permission** — Must be granted manually by the user in system settings
5. **Background data stats** — `NetworkStatsManager` may have a short delay before reporting recent usage

---

## Future Enhancements

- [ ] SMS scam detection with ML
- [ ] Network traffic analysis
- [ ] Push notifications for security alerts
- [ ] Export security reports
- [ ] Real-time permission change detection

---

## License

Copyright (c) 2026 codenamec0de. **All Rights Reserved.**

This source code is viewable for academic and portfolio purposes only. No copying, modification, or distribution is permitted without written permission. See [LICENSE](LICENSE) for details.

---

## Author

Created for UOW Graduation Project
