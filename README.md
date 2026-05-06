# ComicShelf

ComicShelf is a Kotlin Compose Multiplatform app for discovering comic series, viewing issue details, and keeping a personal comic collection. It runs on Android and iOS from a shared Compose UI, with platform-specific camera and web view integrations where needed.

The app uses ComicVine for series, issue, cover, creator, and web-link data. Metron is used for barcode UPC lookup so scanned comics can resolve to ComicVine issue IDs.

## Features

- Browse a home feed of recent, mainstream comic series and issues from ComicVine.
- Search ComicVine volumes by title.
- View series details, issue lists, issue metadata, creators, and cover art.
- Add or remove series and individual issues from a local collection.
- Track issue read status.
- Sort and filter issue lists by collection state.
- Switch collection and search results between grid and list layouts.
- Scan comic barcodes with the device camera and jump to the matching issue.
- Open ComicVine series and issue pages inside the app.

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform and Material 3
- Android and iOS targets
- Ktor client with kotlinx.serialization
- Room KMP with bundled SQLite
- DataStore Preferences
- Coil 3 for image loading
- Android CameraX, ML Kit, and ZXing for barcode scanning
- iOS AVFoundation for barcode scanning

## Project Structure

```text
.
|-- composeApp/
|   |-- src/commonMain/      # Shared Compose UI, navigation, data, APIs, view models
|   |-- src/androidMain/     # Android activity, camera, HTTP engine, platform views
|   |-- src/iosMain/         # iOS Compose controller, camera, HTTP engine, platform views
|   `-- schemas/             # Room schema snapshots
|-- iosApp/                  # SwiftUI iOS host app
|-- gradle/                  # Gradle wrapper and version catalog
`-- local.properties.example # Required API credential template
```

## Requirements

- JDK 11 or newer
- Android Studio with Android SDK 36
- Xcode for iOS builds
- A ComicVine API key
- Metron account credentials for barcode lookup

## API Credentials

Create a `local.properties` file at the repository root:

```properties
METRON_USERNAME=your_metron_username
METRON_PASSWORD=your_metron_password
COMICVINE_API_KEY=your_comicvine_api_key
```

You can also start from the checked-in template:

```sh
cp local.properties.example local.properties
```

`composeApp/build.gradle.kts` reads these values and generates `MetronConfig.kt` during the Gradle build. Keep `local.properties` out of version control because the generated config embeds the credentials into the app binary.

## Running the App

### Android

From the repository root:

```sh
./gradlew :composeApp:installDebug
```

Then launch ComicShelf on a connected Android device or emulator. You can also open the project in Android Studio and run the `composeApp` Android configuration.

### iOS

Open the iOS project in Xcode:

```sh
open iosApp/iosApp.xcodeproj
```

Select an iOS simulator or device, then build and run the `iosApp` scheme. The SwiftUI host app loads the shared Compose UI through `MainViewController`.

## Useful Gradle Commands

```sh
# Build Android debug artifacts
./gradlew :composeApp:assembleDebug

# Compile shared and Android Kotlin
./gradlew :composeApp:compileDebugKotlinAndroid

# Compile iOS simulator Kotlin
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Run common tests
./gradlew :composeApp:allTests
```

## Data and Persistence

ComicTracker stores the user's collection in a local Room database:

- Android database: `comictracker.db`
- iOS database: currently created as `Documents/survey.db`

User display preferences are stored separately with DataStore Preferences, including home tab, collection layout, collection sort, search layout, issue sort order, and issue collection filters.

Room schema snapshots live in `composeApp/schemas/com.moravian.comictracker.data.ComicTrackerDatabase/`.

## Barcode Scanning

The scanner expects comic barcodes that include the supplemental issue code. A plain 12-digit UPC-A code is treated as incomplete because Metron lookup needs the full comic barcode value.

Android scanning uses CameraX with ML Kit and ZXing fallbacks. iOS scanning uses AVFoundation metadata output.

## Notes for Contributors

- Most app behavior lives in shared Kotlin under `composeApp/src/commonMain`.
- Platform-specific implementations use Kotlin `expect`/`actual` files in `androidMain` and `iosMain`.
- API models and clients are in `com.moravian.comictracker.network`.
- Local collection entities and DAOs are in `com.moravian.comictracker.data`.
- Navigation starts in `App.kt`.
- The current common test is only a placeholder, so meaningful unit and integration tests are still an open area.

## Contributors

- Isaac
- Mael
