# PantryMatch 🍳

App Android per lo **scambio di ricette basato sugli ingredienti**: l'utente
registra la propria dispensa e l'app trova le ricette di amici/community
preparabili **subito** o con **al massimo 1–2 ingredienti mancanti**.

Progetto per l'esame di **Programmazione Mobile 2025/26**.
La descrizione completa e la mappatura sulle specifiche d'esame sono in
[`PROGETTO.md`](PROGETTO.md).

---

## Stack

- **Kotlin** + **AGP 9.2.1** / **Gradle 9.4.1** (compileSdk 37, minSdk 24)
- **MVVM**: View (Fragment) ↔ ViewModel (StateFlow) ↔ Repository ↔ Firebase
- **Jetpack**: ViewBinding, Navigation Component, Lifecycle, RecyclerView
- **Coroutine** Kotlin
- **Firebase**: Authentication, Cloud Firestore, Storage
- **Glide** per il caricamento immagini

## Struttura del progetto

```
app/src/main/java/com/example/esamemobile/
 ├─ MainActivity.kt              # single-activity: Toolbar + BottomNav + NavHost
 ├─ data/
 │   ├─ model/                   # Recipe, RecipeIngredient, PantryItem, UserProfile, RecipeMatch
 │   └─ repository/              # AuthRepository, PantryRepository, RecipeRepository
 ├─ ui/
 │   ├─ auth/                    # LoginFragment + AuthViewModel
 │   ├─ pantry/                  # Dispensa: Fragment, ViewModel, Adapter
 │   ├─ search/                  # Ricerca per ingredienti: Fragment, ViewModel, Adapter
 │   ├─ recipe/                  # Dettaglio ricetta: Fragment + ViewModel
 │   ├─ friends/                 # Amici (scheletro)
 │   └─ profile/                 # Profilo + logout
 └─ util/
     ├─ IngredientMatcher.kt     # cuore: matching dispensa ↔ ricette
     └─ Resource.kt              # wrapper stato UI (Loading/Success/Error)

app/src/test/java/.../IngredientMatcherTest.kt   # test unitari del matcher
```

## Build e test

Il progetto usa il JDK incluso in Android Studio. Da terminale:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :app:testDebugUnitTest   # esegue i test del matcher
./gradlew :app:assembleDebug       # genera l'APK debug
```

Oppure apri la cartella in **Android Studio** e usa Run ▶.

> Senza Firebase configurato l'app si avvia e mostra la UI (le 4 sezioni), ma le
> operazioni che leggono/scrivono dati mostreranno un errore finché non completi
> il passo seguente.

---

## ⚙️ Configurazione Firebase (necessaria per login e dati)

1. Crea un progetto su [Firebase Console](https://console.firebase.google.com/).
2. Aggiungi un'app **Android** con package `com.example.esamemobile`.
3. Scarica **`google-services.json`** e mettilo in **`app/google-services.json`**.
4. Abilita il plugin Gradle, decommentando le righe segnalate:
   - in **`build.gradle.kts`** (root): `alias(libs.plugins.google.services) apply false`
   - in **`app/build.gradle.kts`**: `alias(libs.plugins.google.services)`
5. In Firebase Console abilita:
   - **Authentication** → metodi *Email/Password* e *Google*;
   - **Cloud Firestore** (modalità produzione);
   - **Storage**.
6. Pubblica le **Security Rules** in [`firestore.rules`](firestore.rules).

> `app/google-services.json` è in `.gitignore`: ogni membro del gruppo lo scarica
> dal proprio accesso alla console (oppure lo si condivide a parte).

### Indici Firestore

La ricerca usa `whereArrayContainsAny("ingredientNames", …)` combinato con
`whereEqualTo("isPublic", true)` (ed eventualmente `whereIn("authorId", …)`):
al primo avvio Firestore segnalerà nel Logcat un link per creare l'indice
composto necessario. In alternativa definiscilo in `firestore.indexes.json`.

---

## Stato attuale (scheletro)

✅ Architettura, build funzionante, navigazione, modelli, repository, matcher
con test unitari (6/6 verdi).

🔜 Da completare: Google Sign-In nel `LoginFragment`, modulo amici
(`FriendsRepository`), creazione/modifica ricette con foto (Storage),
notifiche/WorkManager (funzionalità background ed extra).
