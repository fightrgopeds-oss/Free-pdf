# Free PDF Reader

A lightweight, private Android PDF reader built with Android's native `PdfRenderer`.

## Compatibility
- Android 9 (API 28) and newer
- No ads, accounts, analytics, or internet permission
- PDFs remain on the device

## Features
- Open PDFs from device storage or another Android app
- Page navigation
- Zoom from 75% to 300%
- Light/dark system theme support
- Opens `application/pdf` links and files

## Build
Open this folder in Android Studio (JDK 17), let Gradle sync, then choose **Build → Build APK(s)**. The debug APK appears under `app/build/outputs/apk/debug/`.

A GitHub Actions workflow is included. Push the folder to GitHub, run **Build Android APK**, and download the `FreePDFReader-debug` artifact.
