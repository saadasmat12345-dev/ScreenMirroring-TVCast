# TVCast

TVCast is a Kotlin + Jetpack Compose Android app for screen mirroring entry points, DLNA/UPnP device discovery, local media browsing, web-video detection, history, favorites, ads, and premium entitlement plumbing.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with Clean Architecture-style core and feature packages
- Hilt, Coroutines, Flow
- Room for history, favorites, and recent devices
- DataStore for settings
- Navigation Compose
- Media3 ExoPlayer for in-app playback
- Google Mobile Ads with test ad unit IDs
- Google Play Billing with placeholder product IDs

## Build Setup

This workspace is configured for the installed local SDK:

- `compileSdk`/`targetSdk`: 36
- Android Gradle Plugin: 8.13.2
- Gradle wrapper: 8.14
- Kotlin: 2.2.21 with KAPT

Use Android Studio's bundled JBR or any supported JDK 17/21 runtime:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

If API 37 and Gradle 9.4.1+ are installed, the dependency set can be upgraded to the newer AGP 9.x/AndroidX 2026 line.

## Current Production Scope

Implemented honestly in this first version:

- Real DLNA/UPnP SSDP discovery for media renderer devices.
- DLNA AVTransport control for direct `http`/`https` media URLs exposed by compatible renderers.
- Android system screen-cast/display settings handoff for mirroring.
- Scoped MediaStore browsing for videos, photos, and music.
- In-app WebView browser with safe settings and direct media URL detection.
- Media3 phone playback route.
- Local-only Room history, favorites, browser history, bookmarks, and recent devices.
- DataStore settings for theme, quality, reconnect, keep-awake, notifications, and homepage.
- Ads/premium managers with centralized rules and placeholders.

Not claimed:

- Chromecast, Roku, Fire TV, AirPlay, DRM bypass, protected stream capture, or universal website support.
- General-purpose web-video downloading.
- Accessibility-service mirroring.
- Broad storage access or `MANAGE_EXTERNAL_STORAGE`.

## Release Configuration

Before Play Store release:

- Replace `BillingProducts` placeholder product IDs in `core/billing/BillingManager.kt`.
- Configure products in Play Console and test purchase acknowledgement/restoration.
- Keep Google Mobile Ads test IDs until production ad units are approved.
- Add a real privacy policy and terms URL in Settings actions.
- Add server-side purchase verification if premium entitlement is business-critical.
- Test on Android 13, 14, 15, and 16 devices.

## Verification Status

Gradle wrapper generation completed. Dependency metadata passed earlier with the API 36-compatible stack, then the project was aligned to Kotlin 2.2.21 because Kotlin 2.4 exceeded Room 2.8.4 processor metadata support and Kotlin 2.1 could not consume newer Billing/Ads metadata. Final full compilation is blocked by local disk space: Gradle repeatedly fills `C:\Users\Saad\.gradle\caches\8.14\transforms` while extracting AARs. I cleared only that generated transform cache after failed attempts so the machine was not left at zero free space. Rerun `.\gradlew.bat assembleDebug` after ensuring substantially more free space on C.

## Physical Device Testing Required

- DLNA discovery on real TVs/renderers from LG, Samsung, Sony, and generic UPnP renderers.
- DLNA AVTransport playback of direct public MP4/HLS URLs.
- Local media playback and scoped permission denial/permanent denial flows.
- Android cast/display settings availability across OEM builds.
- WebView video detection on sites that expose direct non-DRM media.
- Billing test cards, pending purchases, restore, and acknowledgement.
- Mobile Ads native/banner/interstitial frequency and premium suppression.
- Picture-in-picture and orientation behavior during Media3 playback.

## Privacy Checklist

- Browsing history is stored locally in Room only.
- Casting history stores title, URI, media type, device name/id, status, and timestamp.
- No browsing history, media library data, or device list is uploaded by app code.
- Ads SDK and Billing SDK are integrated and must be disclosed in the Play Data Safety form.
- Permissions are requested contextually for nearby devices, media type, and notifications.
- Users can clear browser data and casting history.
- The app does not request `MANAGE_EXTERNAL_STORAGE`.
- The app does not use Accessibility Service for mirroring.
- The app does not bypass DRM, authentication, or website restrictions.
