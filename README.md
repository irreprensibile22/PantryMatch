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
 ├─ MainActivity.kt              # single-activity: Toolbar + BottomNav + NavHost + sessione
 ├─ data/
 │   ├─ model/                   # Recipe, RecipeIngredient, PantryItem, UserProfile, RecipeMatch, Friend
 │   └─ repository/              # Auth, Pantry, Recipe (+Storage), Friends, Favorites
 ├─ ui/
 │   ├─ auth/                    # Login (+Google), Register, AuthViewModel
 │   ├─ pantry/                  # Dispensa: Fragment, ViewModel, Adapter
 │   ├─ search/                  # Home/Feed community + dialog Filtri (Disponibilità/Categoria)
 │   ├─ recipe/                  # Dettaglio + Crea/Modifica: Fragment, ViewModel
 │   ├─ friends/                 # Amici + ricette dell'amico (FriendRecipes): Fragment, ViewModel, Adapter
 │   └─ profile/                 # Profilo + le mie ricette + Preferiti + logout
 ├─ work/                        # Background: PantryMatchWorker + Scheduler (WorkManager)
 └─ util/
     ├─ IngredientMatcher.kt     # cuore: matching dispensa ↔ ricette
     ├─ RecipeFormParser.kt      # parsing form ingredienti/passaggi ↔ testo
     ├─ Notifications.kt         # canale e id delle notifiche locali
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
4. Il plugin Gradle **google-services è già abilitato** (in `build.gradle.kts`
   root e in `app/build.gradle.kts`). ⚠️ Senza `app/google-services.json` la
   build fallisce: se devi avviare il progetto **senza** Firebase, ri-commenta
   `alias(libs.plugins.google.services)` in `app/build.gradle.kts`.
5. In Firebase Console abilita:
   - **Authentication** → metodi *Email/Password* e *Google*;
   - **Cloud Firestore** (modalità produzione);
   - **Storage**.

   > **Google Sign-In**: dopo aver abilitato il provider *Google*, aggiungi
   > l'impronta **SHA-1** dell'app (Impostazioni progetto → la tua app Android →
   > *Aggiungi impronta*) e **ri-scarica** `google-services.json` (ora con
   > `oauth_client` popolato). La SHA-1 di debug si ottiene con:
   > ```bash
   > ./gradlew signingReport
   > ```
   > Il plugin genera la risorsa `default_web_client_id`: senza di essa il
   > pulsante Google si nasconde da solo. Il login Google funge **sia da accesso
   > sia da registrazione** (al primo accesso crea il profilo Firestore), quindi
   > è presente un unico pulsante nella schermata di login.
6. Pubblica le **Security Rules**:
   - Firestore → [`firestore.rules`](firestore.rules);
   - Storage → [`storage.rules`](storage.rules).
7. Pubblica gli **indici compositi** di Firestore da
   [`firestore.indexes.json`](firestore.indexes.json) (vedi sotto).

> `app/google-services.json` è in `.gitignore`: ogni membro del gruppo lo scarica
> dal proprio accesso alla console (oppure lo si condivide a parte).

### Indici Firestore

Gli indici compositi necessari sono già definiti in
[`firestore.indexes.json`](firestore.indexes.json):

- `recipes`: **`authorId` (ASC) + `createdAt` (DESC)** — usato da
  `getMyRecipes()` per "Le mie ricette" ordinate per data;
- `recipes`: **`isPublic` (ASC) + `ingredientNames` (array-contains)** — usato da
  `searchByPantry()` (`whereEqualTo("isPublic", true)` +
  `whereArrayContainsAny("ingredientNames", …)`). Nota: la Home non usa più
  `searchByPantry()` (il filtro mancanti è applicato lato client da
  `IngredientMatcher`); l'indice resta per il metodo, tuttora disponibile in repo.

> Il **feed della Home** (`getFeed`) e le **ricette dell'amico**
> (`getPublicRecipesByAuthor`, `authorId` + `isPublic`) usano solo filtri di
> uguaglianza con ordinamento lato client: **non** richiedono indici compositi.

Pubblicali con `firebase deploy --only firestore:indexes` (Firebase CLI) oppure,
al primo avvio, segui il link che Firestore stampa nel Logcat per crearli dalla
Console.

### Deploy con Firebase CLI

Il file [`firebase.json`](firebase.json) collega rules e indici al deploy:

In [`.firebaserc`](.firebaserc) sostituisci il placeholder
`your-firebase-project-id` con l'ID reale del tuo progetto Firebase, poi:

```bash
firebase login
firebase deploy --only firestore,storage
```

> Senza `.firebaserc` (o per usare un progetto diverso) puoi sempre passare
> `--project <project-id>` ai comandi.

`firebase deploy --only firestore` pubblica `firestore.rules` +
`firestore.indexes.json`; `--only storage` pubblica `storage.rules`.

> Lo Storage delle immagini (`recipe_images/{uid}/…`) è protetto da
> [`storage.rules`](storage.rules): lettura per utenti autenticati, scrittura
> solo nel proprio percorso e con vincoli su tipo (solo immagini) e dimensione
> (< 5 MB).

> **Piano Spark (senza Storage).** Da fine 2024 Firebase Storage richiede il
> piano Blaze. Se lo Storage non è attivo, l'app resta utilizzabile: al
> salvataggio di una ricetta l'upload della foto viene tentato a parte e, se
> fallisce, la ricetta è salvata comunque **senza foto** (con avviso
> `recipe_saved_no_photo`). Le foto si attivano passando a Blaze (free tier
> ampio; consigliato impostare un budget alert).

---

## Stato attuale (sviluppo per step)

Costruzione incrementale, uno step alla volta (ogni step compila):

- ✅ **Step 1 — Registrazione** email/password (validazione formato email + conferma password).
- ✅ **Step 2 — Google Sign-In**: unico pulsante nel login che funge sia da
  accesso sia da registrazione (al primo accesso crea il profilo Firestore); si
  nasconde se manca `default_web_client_id` (provider Google/SHA-1 non configurati).
- ✅ **Step 3 — Sessione**: avvio sul login se non autenticato + logout→login.
- ✅ **Step 4 — Amici**: `FriendsRepository`, ricerca per email, aggiungi/rimuovi
  (il filtro "Solo amici" della ricerca è poi stato sostituito, vedi *Migliorie*).
- ✅ **Step 5 — Ricette CRUD + foto**: editor crea/modifica con immagine su
  **Storage**, "Le mie ricette" nel profilo.
- ✅ **Step 6 — Dettaglio**: menu azioni in action bar — **preferiti**
  (`FavoritesRepository`, sub-collection `users/{uid}/favorites`),
  **condivisione** via intent `ACTION_SEND`, **azioni autore** modifica/elimina
  (con conferma) visibili solo all'autore, **navigazione ricerca→dettaglio**.
- ✅ **Step 7 — Background**: `PantryMatchWorker` (WorkManager, `CoroutineWorker`)
  periodico ogni 12h che confronta la dispensa con le ricette pubbliche e mostra
  una **notifica locale** "Con la tua dispensa puoi preparare N ricette"
  (canale dedicato, permesso `POST_NOTIFICATIONS` richiesto a runtime su
  Android 13+, scheduling unico via `PantryMatchScheduler`).
- ✅ **Step 8 — Firebase**: `firestore.indexes.json` (indici compositi
  `authorId`+`createdAt` e `isPublic`+`ingredientNames`) + `storage.rules`
  (immagini ricette: lettura autenticata, scrittura solo owner con vincoli
  tipo/dimensione).

### Migliorie

- ✅ **Tema arancione (Material 3)**: schema colore personalizzato incentrato
  sull'arancione, con token completi *light* (`values/colors.xml`) e *dark*
  (`values-night/colors.xml`) mappati sugli attributi M3 in `values/themes.xml`
  (primary/secondary/tertiary, container, surface, outline…). Così FAB, pulsanti,
  chip selezionati, indicatore della bottom bar, avatar e accenti diventano
  arancioni in automatico; la **top bar** usa `colorPrimaryContainer` con titolo
  centrato. Rimosso il `values-night/themes.xml` ridondante: la notte eredita lo
  stesso tema con i colori scuri.
- ✅ **Profilo — ricette orizzontali, ricerca testuale e "Vedi tutte"**: "Le mie
  ricette" scorre ora **in orizzontale** (card a larghezza fissa, `RecipeAdapter`
  con flag `horizontal`); ogni sezione ha un **pulsante di ricerca testuale** che
  mostra un campo per filtrare per titolo (Le mie ricette **e** Preferiti, filtro
  client-side). Il pulsante **"Vedi tutte"** apre `RecipesGridFragment`: una
  **griglia** (`GridLayoutManager`, 2 colonne, `item_recipe_grid` +
  `RecipeGridAdapter`) con tutte le proprie ricette e ricerca testuale dedicata
  (`RecipesGridViewModel`).
- ✅ **Amici — conteggio ricette + ricette dell'amico**: ogni card amico mostra
  il **numero di ricette pubbliche** pubblicate (`@plurals/friend_recipe_count`);
  toccando la card si apre `FriendRecipesFragment` con l'**elenco delle sue
  ricette** (stesse schede grandi del feed, con preferiti). Conteggio e lista
  usano `RecipeRepository.getPublicRecipesByAuthor()`; `FriendsViewModel` emette
  `FriendItem(friend, recipeCount)` e la navigazione passa
  `authorId`/`authorName` (titolo della action bar = nome amico).
  La query vincola `authorId` **e** `isPublic == true` (entrambi server-side):
  le Security Rules consentono di leggere le ricette altrui solo se pubbliche,
  quindi senza il vincolo su `isPublic` Firestore rifiuterebbe l'intera query con
  *permission denied* (sintomi: lista in errore e conteggio bloccato a 0). Due
  filtri di sola uguaglianza non richiedono un indice composito.
- ✅ **Home solo community + dialog Filtri per categorie**: la Home mostra sempre
  le ricette della community (rimosso il selettore Community/Amici). Il pulsante
  **Filtri** apre un dialog (`dialog_filters.xml`) **raggruppato in sezioni**:
  *Disponibilità* (ingredienti mancanti: Tutte / Preparabili / Max 1 / Max 2) e
  *Categoria* (chip generati da `@array/recipe_categories`: Antipasto, Primo,
  Secondo, Contorno, Dolce, Bevanda), con *Applica* / *Reimposta*.
  `SearchViewModel` tiene lo stato `maxMissing` + `category` (`setFilters()`,
  filtro categoria client-side su `Recipe.category`). L'editor ricetta usa ora un
  **menu a tendina** sulle stesse categorie (`MaterialAutoCompleteTextView` +
  `app:simpleItems`) così le ricette restano filtrabili.
- ✅ **Feed a schede grandi + salva rapido (regole Sezione 1)**: le card della Home
  sono ora **verticali stile Instagram** — foto a tutta larghezza (240dp), nome
  ricetta, **avatar autore** (cerchio con iniziale) e badge *Preparabile / N
  mancanti* in overlay sulla foto. Il **cuore "salva nei preferiti"** è
  direttamente sulla foto per il salvataggio rapido senza aprire il dettaglio
  (`item_recipe_match.xml`, `RecipeMatchAdapter` con `favoriteIds` +
  `onToggleFavorite`; `SearchViewModel.toggleFavorite()` con update ottimista e
  `FavoritesRepository.getFavoriteIds()`).
- ✅ **Profilo — sezione "Preferiti"**: sotto "Le mie ricette" un secondo elenco
  mostra il **ricettario salvato dalla community** con le stesse schede grandi;
  toccando il cuore si rimuove dai preferiti. Profilo reso scrollabile
  (`NestedScrollView`) con due liste (`fragment_profile.xml`,
  `ProfileViewModel.loadFavorites()/removeFavorite()`,
  `RecipeRepository.getRecipesByIds()`).
- ✅ **Home/Feed come schermata iniziale**: il tab **Cerca** è diventato la
  **Home** dell'app (prima voce della bottom bar, icona casa, start destination
  del nav graph; login/registrazione atterrano qui). All'apertura mostra un
  **feed scrollabile** delle ricette pubbliche dalla più recente
  (`RecipeRepository.getFeed()`, ordinamento lato client per non aggiungere
  indici), annotate col match sulla dispensa così da conservare i badge
  *Preparabile / N mancanti* (`IngredientMatcher.annotate()`, estratto da
  `match()`). La selezione sorgente/filtri è poi confluita nel dialog **Filtri**
  (vedi bullet sopra).
- ✅ **Dispensa — modifica ingrediente**: ogni elemento ha due pulsanti affiancati
  **modifica** ed **elimina**; la modifica apre il dialog pre-compilato e il nome
  viene ri-normalizzato come in inserimento per restare coerente con il matcher
  (`PantryAdapter.onEdit` + `PantryViewModel.updateItem` + `PantryRepository.updateItem`).
- ✅ **Dispensa — unità a tendina**: il campo *Unità* non è più a testo libero ma
  un menu a tendina con le unità principali (`@array/pantry_units`: g, kg, ml, l,
  pz, cucchiaino, cucchiaio, tazza, pizzico, q.b., oltre a "nessuna").
- ✅ **Le mie ricette — azioni rapide**: ogni card nel profilo ha i pulsanti
  **modifica** (apre l'editor con `recipeId`) ed **elimina** (con conferma)
  oltre al tap sulla card che apre il dettaglio
  (`RecipeAdapter.onEdit/onDelete` + `ProfileViewModel.deleteRecipe`).
- ✅ **Storage opzionale (piano Spark)**: se l'upload immagine fallisce, la
  ricetta è salvata comunque senza foto (avviso `recipe_saved_no_photo`).
- 🐛 **Fix ricerca community/amici**: il boolean Kotlin `isPublic` veniva
  serializzato da Firestore come campo `public` (convenzione JavaBean sui getter
  `is…`), perciò `whereEqualTo("isPublic", true)` non trovava nulla. Risolto con
  `@get:/@set:PropertyName("isPublic")` su `Recipe.isPublic` (coerente anche con
  le Security Rules che leggono `isPublic`). Le ricette create prima vanno
  ri-salvate per acquisire il campo corretto.

Cuore funzionale (`IngredientMatcher`) coperto da test unitari (verdi).

> Con il plugin `google-services` abilitato serve `app/google-services.json` per
> compilare. Il pulsante Google resta nascosto finché non si configura il
> provider (e si ri-scarica il `google-services.json` con l'`oauth_client`).
