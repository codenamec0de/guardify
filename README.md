[GITHUB_README (1).md](https://github.com/user-attachments/files/23617989/GITHUB_README.1.md)
# 🛡️ Guardify | App Auditor

> **Your Apps, Your Control** - An Android permission auditing tool that helps users understand and control what their apps can access.

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-blue.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Design-orange.svg)](https://m3.material.io/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-tech-stack)
- [Installation](#-installation)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Architecture](#-architecture)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [Team](#-team)
- [License](#-license)

---

## 🌟 Overview

**Guardify** is an Android application that audits installed apps' permissions, classifies them by risk level, and provides users with actionable insights to enhance their privacy and security. Built with modern Android development tools, Guardify makes permission management accessible to everyone.

### 🎯 Problem Statement

Android apps often request broad permission sets that exceed their actual needs. Most users:
- Don't understand technical permission names
- Face "warning fatigue" from too many alerts
- Lack clear guidance on what actions to take
- Have no easy way to assess overall device security

### 💡 Our Solution

Guardify bridges this gap by:
- **Translating** technical permissions into plain language
- **Classifying** permissions by risk level (High/Medium/Low)
- **Prioritizing** the most important security issues
- **Guiding** users with actionable steps
- **Processing** everything on-device to preserve privacy

### 🏆 Key Differentiators

- ✅ **Standards-Aligned**: Maps to OWASP MASTG and NIST Privacy Framework
- ✅ **Privacy-First**: On-device processing, no data collection
- ✅ **User-Friendly**: Clear explanations, not technical jargon
- ✅ **Actionable**: Direct links to Settings for permission changes
- ✅ **Open & Auditable**: Transparent risk classification rules

---

## ✨ Features

### Current Implementation (V1)

#### 🔐 User Authentication
- Social login support (Google, Facebook, Email)
- Modern, secure authentication UI
- Clean onboarding experience

#### 📊 Security Dashboard
- **Overall Security Score** (0-100 scale)
- Quick statistics: High/Medium/Low risk app counts
- Visual risk indicators
- Quick action buttons for common tasks

#### 📱 App Audit & Management
- **Search Functionality**: Find apps instantly
- **Risk Filters**: Filter by High, Medium, or Low risk
- **App Cards**: Display app name, icon, risk level, permission count, and tracker count
- **Detailed Reports**: 
  - Simple Mode: Risk summary with key points
  - Expert Mode: Detailed permission breakdown
- **Permission Explanations**: Plain-language descriptions with real-world examples

#### 🔔 Smart Alerts
- Security notifications for high-risk behavior
- Camera/microphone access alerts
- New permission additions after updates
- Color-coded severity indicators (Critical/Warning/Info)
- Timestamped activity logs

#### ⚙️ Settings & Preferences
- User profile management
- Premium features preview
- Toggle controls:
  - Push notifications
  - Auto-scan for new apps
  - Tracker detection
- About & support information

### 🎨 Design Features

- **Dark Theme**: Easy on the eyes with vibrant color highlights
- **Material 3**: Latest Material Design components
- **Color-Coded Risks**: 
  - 🔴 High Risk (Red)
  - 🟠 Medium Risk (Orange)
  - 🟢 Low Risk (Green)
- **Smooth Navigation**: Intuitive screen transitions
- **Responsive Layout**: Adapts to different screen sizes
- **Accessibility**: WCAG compliant design

---

## 📸 Screenshots

### Login & Authentication
> Clean, modern login screen with social authentication options

### Dashboard
> Security score at a glance with risk statistics and quick actions

### App List
> Searchable, filterable list of all installed apps with risk indicators

### App Details
> Toggle between Simple and Expert modes for permission details

### Alerts
> Real-time security notifications with color-coded severity

### Settings
> User preferences and app configuration

---

## 🔧 Tech Stack

### Frontend
- **Language**: [Kotlin](https://kotlinlang.org/) - 100% Kotlin codebase
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern declarative UI
- **Design System**: [Material 3](https://m3.material.io/) - Latest Material Design
- **Navigation**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)

### Android Components
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Compile SDK**: API 34

### Libraries & Dependencies
```gradle
- Jetpack Compose BOM 2023.08.00
- Material 3
- Navigation Compose 2.7.6
- Material Icons Extended
- AndroidX Core KTX 1.12.0
- Activity Compose 1.8.2
```

### Development Tools
- **IDE**: Android Studio Hedgehog (2023.1.1+)
- **Build System**: Gradle (Kotlin DSL)
- **Version Control**: Git

### Standards & Frameworks
- **OWASP MASTG**: Mobile App Security Testing Guide
- **NIST Privacy Framework**: Privacy risk assessment
- **Android Guidelines**: Human Interface Guidelines compliance

---

## 📥 Installation

### Prerequisites

Before you begin, ensure you have:
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 8 or higher
- **Android SDK** with API 34
- **Git** (for cloning)

### Option 1: Clone from GitHub

```bash
# Clone the repository
git clone https://github.com/your-username/guardify.git

# Navigate to project directory
cd guardify

# Open in Android Studio
# File → Open → Select the 'guardify' folder
```

### Option 2: Download ZIP

1. Click the green **Code** button above
2. Select **Download ZIP**
3. Extract the ZIP file
4. Open the folder in Android Studio

### Build & Run

1. **Open Project**
   - Launch Android Studio
   - File → Open → Select project folder

2. **Sync Gradle**
   - Wait for automatic Gradle sync (2-5 minutes)
   - Click "Sync Now" if prompted

3. **Run the App**
   - Connect a device or start an emulator
   - Click the green Run button (▶️)
   - Select your device
   - App will install and launch!

### Troubleshooting

**Gradle Sync Failed?**
```bash
# Try invalidating caches
File → Invalidate Caches → Restart
```

**Build Errors?**
```bash
# Clean and rebuild
Build → Clean Project
Build → Rebuild Project
```

**Missing SDK?**
```bash
# Install required SDK
Tools → SDK Manager → Install Android 14 (API 34)
```

---

## 📁 Project Structure

```
guardify/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/guardify/appauditor/
│   │       │   ├── MainActivity.kt                 # App entry point
│   │       │   └── ui/
│   │       │       ├── theme/
│   │       │       │   ├── Color.kt               # Color palette
│   │       │       │   ├── Theme.kt               # Material theme
│   │       │       │   └── Typography.kt          # Text styles
│   │       │       └── screens/
│   │       │           ├── LoginScreen.kt         # Authentication
│   │       │           ├── DashboardScreen.kt     # Main dashboard
│   │       │           ├── AppListScreen.kt       # App list view
│   │       │           ├── AppDetailScreen.kt     # App details
│   │       │           ├── AlertsScreen.kt        # Notifications
│   │       │           └── SettingsScreen.kt      # Settings
│   │       ├── res/                               # Resources
│   │       └── AndroidManifest.xml                # App manifest
│   └── build.gradle.kts                           # App-level build
├── build.gradle.kts                               # Project-level build
├── settings.gradle.kts                            # Gradle settings
├── gradle.properties                              # Gradle config
└── README.md                                      # This file
```

### Key Directories

- **`ui/theme/`**: Design system (colors, typography, theme)
- **`ui/screens/`**: All app screens (6 screens total)
- **`res/values/`**: String resources and XML themes
- **`res/xml/`**: Configuration files (backup, data extraction)

---

## 🚀 Getting Started

### For Users

1. **Download** the APK from [Releases](https://github.com/your-username/guardify/releases)
2. **Install** on your Android device (API 24+)
3. **Open** the app and log in
4. **Scan** your installed apps
5. **Review** risk assessments and take action!

### For Developers

#### Quick Start

```bash
# Clone repository
git clone https://github.com/your-username/guardify.git
cd guardify

# Open in Android Studio
# File → Open → Select folder

# Wait for Gradle sync
# Click Run ▶️
```

#### Run on Emulator

```bash
# Create emulator via Device Manager
Tools → Device Manager → Create Device

# Recommended: Pixel 6, API 34
# Start emulator and run app
```

#### Run on Physical Device

```bash
# Enable Developer Options on your phone
# Enable USB Debugging
# Connect via USB
# Trust computer
# Select device in Android Studio
# Click Run ▶️
```

---

## 🏗️ Architecture

### Design Pattern

Guardify follows **MVVM (Model-View-ViewModel)** architecture principles:

```
┌─────────────────────────────────────┐
│            UI Layer                 │
│  (Jetpack Compose Screens)          │
└─────────────────────────────────────┘
                 ↕
┌─────────────────────────────────────┐
│         ViewModel Layer             │
│  (Business Logic & State)           │
└─────────────────────────────────────┘
                 ↕
┌─────────────────────────────────────┐
│          Data Layer                 │
│  (Repositories & Data Sources)      │
└─────────────────────────────────────┘
```

### Component Breakdown

#### **UI Layer** (Jetpack Compose)
- 6 screen composables
- Reusable UI components
- Material 3 design system
- Navigation management

#### **Theme Layer**
- Centralized color definitions
- Typography system
- Material theme configuration

#### **Navigation**
- Type-safe navigation
- Deep linking support
- Back stack management

### Data Flow

```
User Interaction → UI Event → ViewModel → Data Layer → UI Update
```

---

## 🗺️ Roadmap

### ✅ Phase 1: UI Implementation (COMPLETE)
- [x] Login screen with social auth
- [x] Dashboard with security score
- [x] App list with search & filters
- [x] Detailed app reports (Simple/Expert modes)
- [x] Alerts & notifications UI
- [x] Settings & preferences
- [x] Material 3 dark theme
- [x] Navigation between screens

### 🚧 Phase 2: Core Functionality (IN PROGRESS)
- [ ] PackageManager integration
- [ ] Permission enumeration
- [ ] Risk classification engine
- [ ] OWASP MASTG mapping
- [ ] NIST Privacy Framework alignment
- [ ] Settings deep-links implementation

### 📅 Phase 3: Advanced Features (PLANNED)
- [ ] Exodus API integration (tracker detection)
- [ ] Change detection & alerts
- [ ] CSV/JSON export functionality
- [ ] Category baseline comparisons
- [ ] Privacy coach recommendations
- [ ] Batch permission operations

### 🔮 Phase 4: Premium Features (FUTURE)
- [ ] Real-time monitoring
- [ ] Historical permission tracking
- [ ] Advanced analytics
- [ ] Secure cloud backup
- [ ] Multi-device sync
- [ ] Custom permission profiles

### 🌍 Phase 5: Expansion (FUTURE)
- [ ] Multi-language support
- [ ] Tablet optimization
- [ ] Wear OS companion app
- [ ] API for enterprises
- [ ] Community rule submissions

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute

- 🐛 **Report Bugs**: Submit issues for bugs you find
- 💡 **Suggest Features**: Share ideas for improvements
- 📝 **Improve Docs**: Help make documentation clearer
- 🎨 **Design**: Contribute UI/UX improvements
- 💻 **Code**: Submit pull requests for features or fixes

### Contribution Guidelines

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Code Standards

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful commit messages
- Add comments for complex logic
- Write unit tests for new features
- Ensure all tests pass before submitting
- Update documentation as needed

### Development Setup

```bash
# Fork and clone your fork
git clone https://github.com/your-username/guardify.git

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and test
# Open in Android Studio → Make changes → Test

# Commit and push
git add .
git commit -m "Description of changes"
git push origin feature/your-feature-name

# Open Pull Request on GitHub
```

### Code Review Process

1. Submit PR with clear description
2. Automated checks must pass
3. Maintainers review code
4. Address feedback if needed
5. PR merged once approved!

---

## 👥 Team

### Project Members

| Name | Role | GitHub | Email |
|------|------|--------|-------|
| **Luca Georgiou** | Project Manager | [@lucageo](https://github.com/lucageo) | luca@example.com |
| **Cagri Alaf** | Lead Developer | [@cagrialaf](https://github.com/cagrialaf) | cagri@example.com |
| **Hussain Al Saaid** | UI/UX Developer | [@hussainsaaid](https://github.com/hussainsaaid) | hussain@example.com |
| **Saira Rahman** | Security Analyst | [@sairarahman](https://github.com/sairarahman) | saira@example.com |
| **Tamim Draz** | Market Researcher | [@tamimdraz](https://github.com/tamimdraz) | tamim@example.com |

### Supervisor

**Dr. Joonsang Baek**  
University of Wollongong  
Email: jbaek@uow.edu.au

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024 Guardify Team

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 Contact & Support

### Get in Touch

- 📧 **Email**: guardify.team@example.com
- 🐛 **Issues**: [GitHub Issues](https://github.com/your-username/guardify/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/your-username/guardify/discussions)
- 📱 **Twitter**: [@GuardifyApp](https://twitter.com/guardifyapp)

### Documentation

- 📖 [Wiki](https://github.com/your-username/guardify/wiki)
- 🎓 [Getting Started Guide](docs/GETTING_STARTED.md)
- 🔧 [API Documentation](docs/API.md)
- 🎨 [Design Guidelines](docs/DESIGN.md)

### Community

- 💬 Join our [Discord Server](https://discord.gg/guardify)
- 🌐 Visit our [Website](https://guardify.app)
- 📺 Watch demos on [YouTube](https://youtube.com/@guardifyapp)

---

## 🙏 Acknowledgments

Special thanks to:

- **University of Wollongong** - For project support and supervision
- **OWASP Foundation** - For the Mobile Security Testing Guide
- **NIST** - For the Privacy Framework
- **Android Community** - For amazing libraries and tools
- **All Contributors** - For making this project better!

### Powered By

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Material Design](https://material.io/) - Design system
- [Kotlin](https://kotlinlang.org/) - Programming language
- [Android Studio](https://developer.android.com/studio) - IDE
- [Gradle](https://gradle.org/) - Build automation

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=your-username/guardify&type=Date)](https://star-history.com/#your-username/guardify&Date)

---

## 📊 Project Status

![GitHub Repo stars](https://img.shields.io/github/stars/your-username/guardify?style=social)
![GitHub forks](https://img.shields.io/github/forks/your-username/guardify?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/your-username/guardify?style=social)
![GitHub last commit](https://img.shields.io/github/last-commit/your-username/guardify)
![GitHub issues](https://img.shields.io/github/issues/your-username/guardify)
![GitHub pull requests](https://img.shields.io/github/issues-pr/your-username/guardify)

---

<div align="center">

### 🛡️ Building a Secure Future - Your Apps, Your Control

Made with ❤️ by the Guardify Team

[⬆ Back to Top](#️-guardify--app-auditor)

</div>
