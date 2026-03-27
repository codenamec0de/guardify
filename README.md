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

### V1.2 — Device Security, Breach Checker & App Integrity

#### 1. **Room Database Migration**
- Migrated all persistence from JSON/SharedPreferences to Room (SQLite)
- 6 tables: alerts, monitored_apps, scan_results, device_checks, breach_results, app_settings
- Type-safe DAOs with coroutine support

#### 2. **Persistent Background Service**
- Always-on foreground service (`START_STICKY`) with 10-minute scan loop
- Survives app close — auto-restarts via `BootReceiver` after reboot
- Battery optimization exemption prompt for uninterrupted protection
- WorkManager fallback if the service is killed by the OS

#### 3. **Monitor Tab**
- New bottom navigation tab showing all installed apps sorted by risk
- Per-app enable/disable toggles for background monitoring
- Bulk Enable All / Disable All controls
- Service status indicator (running/stopped)
- Search filtering across monitored apps

#### 4. **Realistic Scan Overhaul**
- Replaced fake animation with real parallel batch scanning (8 concurrent coroutines)
- Live progress: "Scanning: Instagram (23/147)" with smooth animated progress bar
- 3-phase scan: discover apps → analyze permissions → cache to Room database
- Auto-populates monitored apps table after scan

#### 5. **Device Security Check**
- 8-point security audit scoring 0–100:
  - Screen Lock (20pts), Biometric (15pts), Disk Encryption (15pts), OS Version (15pts)
  - Developer Options OFF (10pts), USB Debugging OFF (10pts), Unknown Sources OFF (10pts)
  - Guardify Protection ON (5pts)
- Grade system: Excellent / Good / Fair / At Risk with color coding
- Expandable checklist on Home screen with pass/fail per check
- Results persisted to Room for history tracking

#### 6. **Email Breach Checker (HIBP)**
- Check any email against the Have I Been Pwned database
- Shows breach count, severity (HIGH/MEDIUM/LOW), exposed data types
- Individual breach cards with name, date, description, affected accounts
- Auto-fills logged-in user's email from Firebase Auth
- API key saved locally for convenience
- Results cached in Room database

#### 7. **App Integrity Scanner**
- Per-app security analysis shown in App Detail screen:
  - **Debuggable** — detects debug-mode APKs (CRITICAL)
  - **Cleartext Traffic** — HTTP allowed, vulnerable to MITM (WARNING)
  - **Backup Extraction** — data extractable via ADB (INFO)
  - **Test Build** — dev builds on production device (CRITICAL)
  - **Install Source** — sideloaded vs trusted store (WARNING)
  - **APK Signature** — missing or multiple signing certs (CRITICAL)
- Overall status badge: CLEAN / INFO / WARNINGS / ISSUES FOUND

#### 8. **New App Install Alerts**
- BroadcastReceiver detects `PACKAGE_ADDED` / `PACKAGE_REPLACED`
- Instantly scans new app's permissions + integrity
- Auto-adds to monitored apps (enabled if medium/high risk)
- Sends heads-up notification with risk summary
- Tap notification to view full app detail

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
│   │   ├── MonitoredAppAdapter.kt     # Per-app monitoring toggles
│   │   ├── PermissionAdapter.kt       # Adapter for permission list
│   │   ├── PermissionGroupAdapter.kt  # Expandable category groups
│   │   └── TrackerAdapter.kt          # Adapter for tracker list
│   ├── api/
│   │   ├── ExodusApiService.kt        # Exodus Privacy API interface
│   │   ├── HIBPApiService.kt          # Have I Been Pwned API v3
│   │   └── RetrofitClient.kt          # Retrofit singleton
│   ├── data/
│   │   ├── dao/                       # Room DAOs (Alert, MonitoredApp, ScanResult, etc.)
│   │   ├── entity/                    # Room entities (6 tables)
│   │   └── GuardifyDatabase.kt        # Room database singleton
│   ├── model/
│   │   ├── AppInfo.kt                 # App data model with risk calculation
│   │   ├── PermissionAlert.kt         # Background usage alert model
│   │   ├── PermissionGroup.kt         # Category grouping model
│   │   └── TrackerInfo.kt             # Tracker data models
│   ├── receiver/
│   │   ├── AppInstallReceiver.kt      # New app install detection + alerts
│   │   └── BootReceiver.kt            # Restart service on device reboot
│   ├── service/
│   │   ├── GuardifyMonitorService.kt  # Always-on foreground monitoring service
│   │   └── TestDataUsageService.kt    # Foreground service for detection test
│   ├── ui/
│   │   ├── home/HomeFragment.kt       # Dashboard + device security score
│   │   ├── audit/AuditFragment.kt     # App audit with By App / By Category toggle
│   │   ├── monitor/MonitorFragment.kt # Per-app monitoring toggles
│   │   ├── alerts/AlertsFragment.kt   # Background usage alerts
│   │   └── settings/SettingsFragment.kt
│   ├── util/
│   │   ├── AlertStorage.kt            # Room-backed alert persistence
│   │   ├── AppIntegrityChecker.kt     # Per-app security/tampering analysis
│   │   ├── AppScanner.kt              # Parallel batch app scanner
│   │   ├── BackgroundUsageMonitor.kt  # Core background detection engine
│   │   ├── BatteryOptimizationHelper.kt # Battery exemption utilities
│   │   ├── BreachChecker.kt           # HIBP email breach checker
│   │   ├── DataUsageHelper.kt         # Network stats helper
│   │   ├── DeviceSecurityChecker.kt   # 8-point device security audit
│   │   ├── PermissionHelper.kt        # Permission metadata (exact lookup maps)
│   │   ├── PreferencesManager.kt      # Room + SharedPreferences manager
│   │   └── TrackerRepository.kt       # Exodus API handler
│   ├── worker/
│   │   └── PermissionMonitorWorker.kt # WorkManager periodic fallback scanner
│   ├── LoginActivity.kt
│   ├── OnboardingActivity.kt
│   ├── SplashActivity.kt
│   ├── MainActivity.kt
│   ├── AppDetailActivity.kt
│   ├── BreachCheckerActivity.kt
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
| `FOREGROUND_SERVICE` | Persistent background monitoring service |
| `FOREGROUND_SERVICE_DATA_SYNC` | Android 14+ foreground service type |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Security monitoring service type |
| `POST_NOTIFICATIONS` | Alert notifications (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Restart monitoring after device reboot |
| `WAKE_LOCK` | Keep CPU alive during background scans |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Exempt from Doze for continuous protection |
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
- [ ] Security score history & trend graphs
- [ ] Export PDF security reports
- [ ] VPN-based network monitor

---

## License

Copyright (c) 2026 codenamec0de. **All Rights Reserved.**

This source code is viewable for academic and portfolio purposes only. No copying, modification, or distribution is permitted without written permission. See [LICENSE](LICENSE) for details.

---

## Author

Created for UOW Graduation Project
