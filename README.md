# EWIS Coverage: Android tablet app

This is the full EWIS speaker coverage tool as an Android app (Android 8.0 or later), built for tablets. It includes the 3D model with equipment, plan heatmaps, speakers, racks, walls, doors, noise sources and PDF reports.

On Android:
- **Save design** and **PDF report** open Android's "Save to" picker, so you can save to the tablet, Google Drive or OneDrive. After you save a PDF report, it opens in your PDF viewer.
- **Open design** opens the file picker to load a saved `.json` design.
- **Copy schedule** copies to the clipboard.
- The app keeps your work when you rotate the tablet. Pressing Back asks before closing, because unsaved work is lost.
- Pinch to zoom the page, and use two fingers in the 3D view to zoom and pan.

---

## Get the APK (no software needed): GitHub cloud build

**First time**
1. Sign in at https://github.com (a free account is fine). Click **New repository**, name it `ewis-coverage-android`, set it to **Private** and click **Create repository**.
2. On the new repository page, click **uploading an existing file**.
3. Unzip the download, open the unzipped folder, select **everything inside it** (Ctrl+A) and drag it into the browser. That includes the `.github` folder, `app`, `build.gradle`, `settings.gradle` and the rest. Click **Commit changes**.
4. Open the **Actions** tab. **Build Android app** starts by itself. It takes about 5 minutes.
5. Open the finished run (green tick). Under **Artifacts**, download **EWIS-Coverage-Android**. The zip contains `EWIS-Coverage.apk`.

**Updating with a new version**
1. In your repository, click **Add file › Upload files**.
2. Drag in everything from the new unzipped folder. Files with the same name are replaced. Click **Commit changes**.
3. The build runs again by itself. Download the new APK from **Actions** as above, and install it over the old app. Your saved design files aren't affected.

**If the Actions tab shows no build**
The `.github` folder didn't upload, which happens on Macs because Finder hides folders whose names start with a dot. Click **Add file › Create new file** and type the name `.github/workflows/build-android.yml`. Open the file `WORKFLOW - paste into .github-workflows-build-android.yml.txt` from this folder, copy all of it into the editor, and click **Commit changes**.

It doesn't matter if the files end up one folder down in the repository. The build finds the project either way.

## Install it on your tablet

1. Get `EWIS-Coverage.apk` onto the tablet. You can email it to yourself, put it on Google Drive or OneDrive, or copy it over USB. You may need to unzip the download first.
2. On the tablet, tap the `.apk` file. Android asks you to allow installs from that app (for example Files, Chrome or Gmail). Tap **Settings**, turn on **Allow from this source**, then go back and tap **Install**.
3. If Play Protect warns that the app is from an unknown developer, tap **More details › Install anyway**. This happens because the app isn't from the Play Store.
4. Open **EWIS Coverage** from the app drawer.

A work-managed tablet may block installs from outside the Play Store. If so, IT needs to allow it.

## Alternative: build with Android Studio

1. Install Android Studio from https://developer.android.com/studio.
2. Choose **File › Open** and select this folder. Let it sync; it downloads what it needs.
3. To bundle the 3D library so the app works offline, download these two files into `app/src/main/assets/www/`:
   - https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js
   - https://cdn.jsdelivr.net/npm/three@0.128.0/examples/js/controls/OrbitControls.js
   If you skip this, the app loads the 3D library from the internet on first use. The cloud build does this step for you.
4. Plug the tablet in with USB debugging enabled and press **Run**, or use **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.

## About the signing key

`app/ewis-signing.keystore` is the key that signs the app. Keep it in the repository. Every build uses it, so new versions install over the old app on your tablet. Keep the repository private; the key is only for installing the app yourself, not for the Play Store.

## Updating the app

The whole tool is in `app/src/main/assets/www/index.html`. Replace that file with a newer version, keeping the small `AndroidBridge` script at the top of `<head>`. Increase `versionCode` in `app/build.gradle`, then build again. Installing the new APK over the old one keeps the app in the same place on your tablet.

## Project layout

```
app/src/main/java/com/ewis/coverage/MainActivity.java   WebView host; saving, opening and clipboard
app/src/main/assets/www/index.html                      The coverage tool
app/src/main/AndroidManifest.xml                        App setup (internet access for the 3D library)
app/src/main/res/                                       Icon, name, theme
.github/workflows/build-android.yml                     The cloud build
```

## Limitations

This is a design-stage SPL check. It uses free-field inverse-square spreading, an off-axis approximation, rack and wall losses along the straight path, and an optional Sabine reverberant field. It does not predict STI under AS 1670.4 and does not replace measurement at commissioning.
