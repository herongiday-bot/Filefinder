FILE FINDER — Android starter project
=====================================

What this first version does
- Premium dark interface with orange accents.
- Lets the user select one or more folders using Android's folder picker.
- Keeps folder access between app launches where Android permits.
- Searches recursively by partial filename, including extensions.
- Shows file size and modified date.
- Opens and shares files using installed Android apps.
- Offers delete for items Android reports as writable.
- Live search starts after two characters; SEARCH button also works.
- Works offline.

Important Android storage limitation
------------------------------------
This version searches the folders the user explicitly selects. Android does not
allow an ordinary app to silently scan every protected location on the phone.
Some system/app-private folders and restricted locations cannot be selected.
To search broadly, select Internal storage (if Android offers it) or add folders
such as Download, Documents, DCIM, Pictures, Movies and Music individually.

Build
-----
1. Install Android Studio on Windows.
2. Extract this ZIP.
3. In Android Studio choose Open, then select the extracted FileFinder folder.
4. Allow Gradle to sync. Internet is needed the first time to download Gradle
   and the AndroidX DocumentFile dependency.
5. Connect the Samsung phone with USB and enable Developer options + USB
   debugging, or use an Android emulator.
6. Press Run.

Requirements
------------
Android Studio with JDK 17 support.
The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compile SDK 35,
and minimum Android 8.0 (API 26).

This is a starter project, not a signed Play Store release APK.
