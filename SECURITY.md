# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of Guardify seriously. If you believe you have found a security vulnerability, please report it to us as described below.

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them by creating a private security advisory on GitHub or by contacting us directly.

### What to Include

Please include the following information:

- Type of issue (e.g., buffer overflow, SQL injection, cross-site scripting, etc.)
- Full paths of source file(s) related to the manifestation of the issue
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit the issue

### Response Timeline

- We will acknowledge your report within 48 hours
- We will provide a detailed response within 7 days
- We will work to fix verified vulnerabilities as quickly as possible

## Security Measures

Guardify implements the following security measures:

- **Local Processing**: All app scanning and analysis happens locally on your device
- **No Data Transmission**: We do not collect or transmit your app data to any server
- **Firebase Authentication**: Secure authentication using industry-standard Firebase Auth
- **HTTPS Only**: All API communications use HTTPS encryption
- **No Analytics**: We do not include any analytics or tracking SDKs

## Best Practices for Users

1. Only download Guardify from official sources (this GitHub repo)
2. Verify the APK signature before installation
3. Keep the app updated to the latest version
4. Review permissions granted to the app
