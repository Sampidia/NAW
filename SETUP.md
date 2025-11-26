# Naija Ayo Worldwide - Setup Instructions

## Firebase Configuration

This project uses Firebase for backend services. To set up the project:

### 1. Get Your Firebase Configuration

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (or create a new one)
3. Navigate to **Project Settings** → **General**
4. Scroll down to **Your apps** section
5. Click on your Android app (or add one if needed)
6. Download the `google-services.json` file

### 2. Add Configuration to Project

1. Place the downloaded `google-services.json` file in the `app/` directory
2. **Important**: This file is gitignored for security - never commit it to version control
3. A template file `app/google-services.json.example` is provided for reference

### 3. Build and Run

1. Ensure `google-services.json` is in the `app/` directory
2. Sync Gradle files in Android Studio
3. Build and run the project

## Security Notes

- **Never commit** `google-services.json` to version control
- Keep your API keys secure
- Use API key restrictions in Google Cloud Console
- Rotate keys immediately if exposed

## API Key Restrictions (Recommended)

To secure your Firebase API keys:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Navigate to **APIs & Services** → **Credentials**
4. Click on your Android API key
5. Add restrictions:
   - **Application restrictions**: Set to "Android apps"
   - **Add your app's package name**: `com.naijaayo.worldwide`
   - **Add your SHA-1 fingerprint** (get from Android Studio or `keytool`)
   - **API restrictions**: Limit to only Firebase APIs you're using

## Troubleshooting

If you encounter build errors:
- Verify `google-services.json` is in the correct location (`app/` directory)
- Check that the package name matches: `com.naijaayo.worldwide`
- Ensure Firebase services are enabled in your Firebase project
- Sync Gradle files and clean/rebuild the project

## Database Rules

This project uses Firebase Realtime Database. You must deploy the security rules for the app to function correctly.

1. Locate the `firebase-database-rules.json` file in the root directory.
2. Copy the contents of this file.
3. Go to [Firebase Console](https://console.firebase.google.com/) -> **Realtime Database** -> **Rules**.
4. Paste the rules and click **Publish**.

**Important:** The rules include necessary index configurations and read permissions for room joining.
