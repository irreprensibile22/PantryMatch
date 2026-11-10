# PantryMatch

PantryMatch is an Android app that tells you what you can cook with what you already have.
Add the ingredients in your pantry (manually or by scanning a barcode), and the app matches
them against a community recipe feed, showing which recipes are ready to cook and which are
only a few ingredients away.

> The app UI is in Italian. Code, comments and documentation are in English.

## Features

- **Pantry** – add, edit and remove ingredients with quantity and unit. Ingredient names come
  from a shared catalog so they stay consistent.
- **Barcode scanning** – scan a product with the Google Play Services code scanner; the product
  is resolved through the public [Open Food Facts](https://world.openfoodfacts.org/) API.
- **Recipe matching** – every recipe is annotated with the ingredients you are missing.
  Results are sorted by missing count, then by preparation time. Optional ingredients never
  count as missing, and common Italian plurals are normalized (e.g. `uova` → `uovo`).
- **Community feed** – browse public recipes, filtered by availability (ready to cook / up to N
  missing) and by category.
- **Recipe book** – create, edit and delete your own recipes (with photo, public/private flag),
  and keep a list of saved favorites.
- **Friends** – find other users by email, add them as friends and browse their public recipes.
- **Notifications** – a periodic background job (WorkManager, every 30 minutes, network
  required) notifies you when recipes become ready to cook with your pantry. It can be turned
  off from the profile screen.
- **Account** – email/password and Google sign-in, change name, email and password.

## Tech stack

| Area | Library |
| --- | --- |
| Language | Kotlin, coroutines + Flow |
| UI | Android Views, ViewBinding, Material 3, Navigation Component, RecyclerView |
| Architecture | MVVM (Fragment → ViewModel → Repository) |
| Backend | Firebase Authentication, Cloud Firestore, Cloud Storage |
| Images | Glide |
| Background work | WorkManager |
| Barcode | Google Play Services Code Scanner + Open Food Facts API |
| Tests | JUnit 4, kotlinx-coroutines-test |

Requirements: Android 7.0 (API 24) or newer. The project targets and compiles against API 37.

## Project structure

```
app/src/main/java/com/example/pantrymatch/
├── MainActivity.kt        # single Activity: toolbar, bottom navigation, NavHost
├── data/
│   ├── model/             # Firestore data classes (Recipe, PantryItem, UserProfile, ...)
│   └── repository/        # Firebase and Open Food Facts access
├── ui/                    # one package per screen: auth, pantry, search (Home),
│                          # recipe, recipebook, friends, profile
├── util/                  # IngredientMatcher, Resource, preferences, notifications
└── work/                  # periodic PantryMatchWorker and its scheduler
```

Firebase configuration lives in the repository root:

- `firestore.rules` – users can only write their own data; public recipes are readable by any
  signed-in user.
- `storage.rules` – recipe images under `recipe_images/{uid}/`, owner-only writes, images
  under 5 MB.
- `firestore.indexes.json` – composite indexes used by the recipe queries.

## Getting started

### 1. Create a Firebase project

1. Create a project in the [Firebase console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.example.pantrymatch`.
3. Download `google-services.json` and put it in `app/`. The file is git-ignored.
4. Enable **Authentication** with the *Email/Password* and *Google* providers.
   For Google sign-in, add your debug SHA-1 fingerprint to the Firebase app settings
   (`./gradlew signingReport` prints it).
5. Create a **Cloud Firestore** database.
6. Optional: enable **Cloud Storage** for recipe photos. Storage requires the Blaze plan;
   without it, recipes are saved without a photo.

### 2. Deploy rules and indexes

Install the [Firebase CLI](https://firebase.google.com/docs/cli), set your project id in
`.firebaserc` (replace `your-firebase-project-id`), then run:

```bash
firebase deploy --only firestore:rules,firestore:indexes,storage
```

### 3. Build and run

Open the project in a recent Android Studio, or build from the command line:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew installDebug         # install on a connected device or emulator
./gradlew testDebugUnitTest    # run unit tests
```

The `google-services` Gradle plugin needs `app/google-services.json`. To build without
Firebase, comment out the plugin line in `app/build.gradle.kts`. The Google sign-in button is
hidden automatically when no web client id is available.

## Tests

`app/src/test/.../IngredientMatcherTest.kt` covers the matching logic: name normalization,
missing-ingredient counting, optional ingredients, sorting and filtering.

## License

Released under the [MIT License](LICENSE).
