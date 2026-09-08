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
 │   └─ repository/              # Auth, Pantry, Recipe (+Storage), Friends, Favorites, ProductLookup (Open Food Facts)
 ├─ ui/
 │   ├─ auth/                    # Login (+Google), Register, AuthViewModel
 │   ├─ pantry/                  # Dispensa: Fragment, ViewModel, Adapter
 │   ├─ search/                  # Home/Feed community + dialog Filtri (Disponibilità/Categoria)
 │   ├─ recipe/                  # Dettaglio + Crea/Modifica: Fragment, ViewModel
 │   ├─ friends/                 # Amici + ricette dell'amico (FriendRecipes): Fragment, ViewModel, Adapter
 │   ├─ recipebook/              # Ricettario: le mie ricette + Preferiti (+ "Vedi tutte")
 │   └─ profile/                 # Profilo: dati utente + logout (+ griglia "Le mie ricette")
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
  periodico ogni 30 min che confronta la dispensa con le ricette pubbliche e mostra
  una **notifica locale** "Con la tua dispensa puoi preparare N ricette"
  (canale dedicato, permesso `POST_NOTIFICATIONS` richiesto a runtime su
  Android 13+, scheduling unico via `PantryMatchScheduler`).
- ✅ **Step 8 — Firebase**: `firestore.indexes.json` (indici compositi
  `authorId`+`createdAt` e `isPublic`+`ingredientNames`) + `storage.rules`
  (immagini ricette: lettura autenticata, scrittura solo owner con vincoli
  tipo/dimensione).

### Migliorie

- ✅ **Dispensa — inserimento tramite codice a barre (fotocamera + API esterna)**:
  nella Dispensa un **mini-FAB "scansiona"** (in basso a sinistra, opposto al FAB
  "Aggiungi") apre lo scanner di **ML Kit** via *Google code scanner*
  (`play-services-code-scanner`): UI di scansione pronta di Google Play Services,
  **senza permesso `CAMERA`** nel manifest (lo scanner gira in un processo
  separato). Il codice letto (EAN-13/EAN-8/UPC-A/UPC-E/QR/Code 128) viene risolto
  interrogando l'**API pubblica e gratuita Open Food Facts**
  (`ProductLookupRepository`, `HttpURLConnection` + `org.json`, nessuna dipendenza
  HTTP extra). Il lookup restituisce **nome commerciale** (`product_name_it`/
  `product_name`) **e categoria** (ultima voce `en:` di `categories_tags`,
  ripulita): dato che la categoria è più vicina a un ingrediente utile al
  matching, il dialog **"Aggiungi ingrediente" pre-compila il campo nome con la
  categoria** (passata per `IngredientMatcher.normalize()`) e mostra il **nome
  commerciale come testo di aiuto** ("Prodotto: …") sotto il campo; entrambi
  restano editabili, così l'utente corregge se la categoria non va bene (spesso è
  in inglese perché Open Food Facts non fornisce l'italiano in modo affidabile).
  Se manca la categoria si usa il nome commerciale nel campo.
  `PantryViewModel.lookupBarcode()` espone l'esito come evento one-shot
  (`SharedFlow`): *Found* (dialog pre-compilato nome + helper), *NotFound* (avviso
  + dialog vuoto), *Error* (avviso rete). Copre due voci **Extra** (fotocamera +
  API esterna). _Nota: lo scanner richiede Google Play Services; su emulatore la
  lettura funziona solo iniettando un'immagine nella scena virtuale — meglio
  testare su dispositivo fisico._
- ✅ **Login e Registrazione in stile "Tavola"**: le schermate di accesso e
  registrazione adottano il look editoriale crema come il resto dell'app. Entrambe
  (`fragment_login`/`fragment_register`) sono scrollabili su fondo `tavola_bg`, con
  **logo** (`ic_logo`, la pentola col check) + **titolo serif** (accent su login,
  ink su registrazione) e **sottotitolo** (`login_subtitle`/`register_subtitle`); i
  campi sono **OutlinedBox in card** (`TavolaEditorCard`) con icone iniziali
  (`ic_mail`/`ic_lock`/`ic_profile`) e i pulsanti primari sono **accentati**
  (terracotta, angoli 14dp). Il login ha un separatore **"oppure"** (hairline) tra
  il pulsante Accedi e il **bottone Google** (outline), più il link accentato
  "Non hai un account? Registrati". Tutti gli ID dei campi/bottoni restano invariati.
- ✅ **Catalogo ingredienti a tendina (uniforma i nomi)**: inserendo un ingrediente
  — sia nella **Dispensa** sia in una **ricetta** — un **menu a tendina** suggerisce
  da un **catalogo condiviso** (`@array/ingredient_catalog`, ~70 voci in forma
  canonica) così da uniformare i nomi ed evitare doppioni (es. *uovo*/*uova*). Il
  campo nome del dialog (`dialog_add_ingredient`) è ora un `MaterialAutoCompleteTextView`
  con `app:simpleItems` (digitazione libera comunque consentita). Nell'**editor
  ricetta** la vecchia textarea a testo libero degli ingredienti è stata sostituita
  da una **lista strutturata**: il pulsante "Aggiungi ingrediente" apre lo **stesso
  dialog** della Dispensa (nome dal catalogo + quantità + unità) e ogni ingrediente
  diventa un **chip rimovibile** (tap = modifica, X = rimuovi) in
  `chipGroupIngredients`; `RecipeEditFragment` mantiene la lista in memoria e la usa
  al salvataggio (niente più parsing testuale). Le forme canoniche del catalogo sono
  coerenti con `IngredientMatcher.normalize()`/`SYNONYMS`, che continua a ricondurre
  i plurali al singolare lato matching.
- ✅ **Editor, Dettaglio e dialog Ingrediente in stile "Tavola"**: le ultime
  schermate ancora in Material "piatto" adottano il look editoriale crema. *Editor*
  (`fragment_recipe_edit`): fondo `tavola_bg`, **foto in card arrotondata** con
  pulsante "Scegli foto" accentato in overlay, campi raggruppati in **sezioni serif**
  (Dettagli/Ingredienti/Procedimento/Pubblicazione) dentro card morbide
  (`TavolaEditorCard`/`TavolaEditorSection`), input **OutlinedBox**, switch "Pubblica"
  in card e pulsante **Salva** terracotta a tutta larghezza. *Dettaglio*
  (`fragment_recipe_detail`): foto **hero**, titolo serif, **riga meta a pillole**
  (categoria · tempo · difficoltà, `TavolaMetaPill` su `bg_chip_recipes`, popolata da
  `RecipeDetailFragment.bindMeta()` con `prep_time_format` e le label difficoltà) e
  sezioni Ingredienti/Procedimento in card. *Dialog Aggiungi/Modifica ingrediente*
  (`dialog_add_ingredient`): campi OutlinedBox arrotondati con icona dispensa. La
  toolbar globale resta su Editor e Dettaglio (back + azioni preferiti/condividi/
  modifica/elimina e salvataggio) ma il corpo è ora coerente con il resto dell'app.
- ✅ **Splash screen con logo dedicato**: all'avvio l'app mostra uno **splash
  screen** (API moderna `androidx.core:core-splashscreen`, Android 12+ nativo e
  back-compat sulle versioni precedenti) con un **logo vettoriale apposito**
  (`drawable/ic_logo.xml`): una **pentola** con coperchio, manici e vapore segnata
  da un **check** = il "match" tra dispensa e ricette preparabili, cuore dell'app.
  Tinte della **palette "Tavola"** (terracotta `#C8552B`/`#8C4A30` su sfondo crema
  `splash_background` `#F4ECE0`); il disegno è scalato nella zona sicura del cerchio
  splash per non essere tagliato. Tema `Theme.ESAMEMOBILE.Splash`
  (`windowSplashScreenBackground` + `windowSplashScreenAnimatedIcon`, con
  `postSplashScreenTheme` che torna al tema principale), assegnato all'activity di
  lancio nel manifest; `MainActivity` chiama `installSplashScreen()` prima di
  `super.onCreate()`. Il tema principale dichiara un `android:windowBackground`
  crema esplicito, così la finestra dipinge crema durante la transizione
  post-splash. Il logo è riutilizzabile anche altrove nell'UI. La stessa
  identità è applicata all'**icona dell'app** (adaptive-icon): `ic_launcher_background`
  diventa terracotta pieno e `ic_launcher_foreground` disegna la pentola crema col
  check (sostituendo il robottino Android di default). _Nota: l'adaptive-icon vale
  su Android 8+ (API 26); i bitmap legacy `mipmap-*/ic_launcher.webp` per API 24–25
  vanno rigenerati da Android Studio (Image Asset) se si punta a quei livelli._
- ✅ **Sotto-schermate a lista in stile "Tavola"**: le due sotto-schermate che
  ancora usavano la top bar globale — **Ricette dell'amico** (`FriendRecipesFragment`)
  e **Griglia "Vedi tutte"** (`RecipesGridFragment`) — adottano ora lo stesso
  **header editoriale interno** delle 5 schermate principali (sfondo crema
  `tavola_bg`, header `tavola_header` con titolo serif + sottotitolo + hairline) con
  una **freccia indietro** dedicata (`ic_arrow_back`, cablata a `navigateUp()`); la
  **top bar globale è nascosta** anche qui (`MainActivity` aggiunge
  `friendRecipesFragment`/`recipesGridFragment` alla lista `ownHeader`). Il titolo è
  popolato a runtime (nome dell'amico / "Le mie ricette" o "Salvati" per modalità) e
  la ricerca della griglia usa la **search box stile Tavola** (`bg_search_box`).
  Dettaglio ed Editor restano con la toolbar (necessaria per back + azioni
  preferiti/condividi/modifica/elimina e salvataggio) ma poggiano già sul fondo
  crema (`android:colorBackground` = `#F4ECE0`).
- ✅ **Profilo — gestione account**: la schermata Profilo ha una sezione **Account**
  con **modifica nome**, **cambio email**, **cambio password** e **reset password
  via email**. Logica in `AuthRepository` (`updateDisplayName` → FirebaseAuth +
  doc Firestore con `SetOptions.merge()`; `changePassword`/`changeEmail` con
  **re-autenticazione** via `EmailAuthProvider` perché Firebase richiede un accesso
  recente; il cambio email usa `verifyBeforeUpdateEmail`, quindi diventa effettivo
  solo dopo la conferma dal link; `sendPasswordReset`). `ProfileViewModel` espone
  gli esiti come **eventi one-shot** (`SharedFlow`), il `ProfileFragment` apre
  dialog dedicati (`dialog_edit_name`/`dialog_change_password`/`dialog_change_email`)
  con validazione inline. Poiché `verifyBeforeUpdateEmail` non notifica l'avvenuto
  cambio, l'email del profilo Firestore viene **riallineata best-effort** con
  `AuthRepository.reconcileEmail()` (fa `reload()` dell'utente e, se l'email
  verificata differisce dal documento, la aggiorna in lowercase): invocata
  all'avvio app (`MainActivity`, se loggato) e al rientro nel Profilo
  (`onResume`), con avviso "Email aggiornata" quando scatta. Cambiando l'email
  primaria Firebase **revoca la sessione**: `reconcileEmail()` forza un refresh del
  token (`getIdToken(true)`), intercetta `FirebaseAuthInvalidUserException`, fa
  `signOut()` e ritorna `EmailSync.SessionRevoked`; la UI mostra "Accedi di nuovo"
  (Toast) e **reindirizza al login** (sia da `MainActivity` sia dal Profilo),
  evitando l'errore "utente non autorizzato". L'utente è anche avvisato in anticipo
  ("dovrai accedere di nuovo") al momento della richiesta di cambio. _Nota: cambio
  email/password valgono per account email/password; la foto profilo richiederebbe
  Storage/Blaze._
- ✅ **Profilo e Ricettario separati**: la logica del profilo è ora distinta da
  quella delle ricette. Il **Profilo** (`ProfileFragment`) mostra solo avatar,
  nome, email e **logout**. Il nuovo **Ricettario** (`RecipeBookFragment` +
  `RecipeBookViewModel`, package `ui.recipebook`) raccoglie le proprie ricette e i
  preferiti (poi evoluto nei **tab "Le mie ricette / Salvati"** — vedi bullet
  "Ricettario e Profilo — redesign Tavola"), più il FAB "Aggiungi ricetta". È una
  **5ª voce della bottom bar**
  (`Ricettario`, `ic_recipe_book`), destinazione top-level del nav graph con le
  proprie azioni (`action_recipebook_to_detail/_to_edit/_to_recipes_grid`).
  `ProfileViewModel` è stato sostituito da `RecipeBookViewModel`. Nella Home è
  stato inoltre rimosso il sottotitolo "Cucinate dai tuoi amici".
- ✅ **Ricettario e Profilo — redesign "Tavola" (mockup dedicato)**: entrambe le
  schermate seguono `Tavola Ricettario & Profilo.dc.html`, con **top bar globale
  nascosta** (header interno) anche qui.
  *Ricettario* (`fragment_recipebook` + `RecipeBookFragment`): header con titolo
  serif "Il tuo ricettario", sottotitolo dinamico e **tab segmentati**
  (`TabLayout`) **Le mie ricette / Salvati**. "Le mie ricette" sono card a tutta
  larghezza in stile feed (foto, **badge stato Pubblica/Privata**, tempo + n°
  ingredienti, azioni modifica/elimina circolari) tramite `item_recipe` +
  `RecipeAdapter` riscritti; "Salvati" riusa le schede `item_recipe_match` con
  cuore per rimuovere dai preferiti. **FAB pillola "Aggiungi ricetta"** solo sul
  tab proprie ricette. Sono mantenuti **ricerca testuale** (un'unica barra che
  filtra il tab attivo per titolo) e **"Vedi tutte"** su entrambi i tab →
  `RecipesGridFragment`, ora **parametrico** (`argument mode = mine|saved`):
  mostra in griglia le proprie ricette o i salvati (`RecipesGridViewModel.Mode`,
  caricando i preferiti via `getFavoriteIds()` + `getRecipesByIds()`), con titolo
  della top bar coerente.
  *Profilo* (`fragment_profile` + `ProfileFragment`): **hero** (avatar con bordo,
  nome serif, email), **statistiche** Ricette/Salvate/Amici
  (`ProfileViewModel.loadStats()` su Recipe/Favorites/Friends repository), sezione
  **Account** a righe (tile icona + label + chevron, `item_account_row`) che
  riaprono i dialog esistenti, sezione **Preferenze** con **toggle Notifiche**
  (persistito in `SettingsPrefs`, pianifica/annulla `PantryMatchScheduler`;
  `MainActivity` rispetta la preferenza all'avvio), **logout** e footer versione.
  Stili condivisi in `values/styles.xml` (`TavolaStatCard`, `TavolaListCard`, …).
- ✅ **Tutta l'app sul tema "Tavola"**: i token Material 3 *light*
  (`values/colors.xml`) sono ritarati sulla palette calda crema/terracotta
  (background `#F4ECE0`, surface `#FFFCF8`, primary `#C8552B`, primaryContainer
  `#F7E4D4`), così **le schermate che usano gli attributi del tema si adattano da
  sole**: top bar, Profilo, Ricettario, Dettaglio, Editor, Ricette dell'amico,
  Griglia, Login/Registrazione, dialog e input. Le card delle ricette
  (`item_recipe`, `item_recipe_grid`) passano allo stile Tavola (angoli 18dp,
  superficie crema, bordo sottile, titolo serif) e il **Dettaglio** ha titolo e
  intestazioni serif; il titolo del **Login** è serif accentato. L'app è forzata in
  **tema chiaro** (`AppCompatDelegate.MODE_NIGHT_NO` in `MainActivity`) perché il
  look Tavola è chiaro/caldo (Home/Dispensa/Amici usano comunque la palette crema).
- ✅ **Dispensa e Amici — redesign "Tavola"**: anche Dispensa e Amici adottano il
  look editoriale (mockup `Tavola_Dispensa_Amici.html`) con **header dedicato**
  (logo + avatar iniziale + titolo serif + sottotitolo/conteggio) e **top bar
  globale nascosta** anche su questi due tab (`MainActivity`). *Dispensa*: card
  crema con tile-icona arrotondata, nome serif, quantità e pulsanti circolari
  modifica (neutro) / elimina (rosso tenue); header con conteggio dinamico
  (`@plurals/pantry_count`); FAB tondo accentato. *Amici*: search box stilizzata,
  etichetta sezione "AMICI · n" (`friends_label`), card con avatar iniziale, nome
  serif, email e chip "N ricette pubblicate" (icona libro), pulsante rimuovi
  circolare; le card dei risultati ricerca (`item_user_search`) hanno avatar e
  pulsante "+" accentato. Nuovi drawable: `bg_circle_neutral/_danger`,
  `bg_ingredient_icon`, `bg_chip_recipes`, `bg_search_box`, `ic_book_small`.
- ✅ **Home — redesign editoriale "Tavola"**: la Home adotta il look del mockup
  `Tavola_Home_A.html` — palette calda crema (`tavola_*` in `values/colors.xml`),
  **header dedicato** nel fragment (logo *Tavola* serif + avatar con l'iniziale
  dell'utente, titolo "Ricette dalla community" serif, sottotitolo + pillola
  *Filtri* outline) con la **top bar globale nascosta sulla Home**
  (`MainActivity` toggle su `appBar`). Le card del feed (`item_recipe_match.xml`)
  diventano schede crema con angoli 22dp, badge a pillola (*Preparabile* oliva /
  *Mancante* terracotta), cuore "salva" su scrim chiaro, titolo serif e riga
  autore con avatar + tempo di preparazione (icona orologio `ic_clock`). La
  **bottom bar** è ricolorata in crema senza pillola indicatore
  (`nav_item_color`, `TavolaNavIndicator`).
- ✅ **Tema Material 3 (base poi evoluta in "Tavola")**: schema colore
  personalizzato con token mappati sugli attributi M3 in `values/themes.xml`
  (primary/secondary/tertiary, container, surface, outline…). Così FAB, pulsanti,
  chip selezionati, indicatore della bottom bar, avatar e accenti seguono il tema
  in automatico; la **top bar** usa `colorPrimaryContainer` con titolo centrato.
  _I token light sono poi stati ritarati sulla palette calda "Tavola" e l'app è
  forzata in tema chiaro — vedi bullet "Tutta l'app sul tema Tavola". Il file
  `values-night/colors.xml` resta in repo ma non è più applicato._
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
- ✅ **Ricettario — sezione "Preferiti"**: sotto "Le mie ricette" un secondo elenco
  mostra il **ricettario salvato dalla community** con le stesse schede grandi;
  toccando il cuore si rimuove dai preferiti. Schermata scrollabile
  (`NestedScrollView`) con due liste (`fragment_recipebook.xml`,
  `RecipeBookViewModel.loadFavorites()/removeFavorite()`,
  `RecipeRepository.getRecipesByIds()`). _(prima dentro il Profilo, ora separata —
  vedi bullet "Profilo e Ricettario separati".)_
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
- ✅ **Le mie ricette — azioni rapide**: ogni card nel Ricettario ha i pulsanti
  **modifica** (apre l'editor con `recipeId`) ed **elimina** (con conferma)
  oltre al tap sulla card che apre il dettaglio
  (`RecipeAdapter.onEdit/onDelete` + `RecipeBookViewModel.deleteRecipe`).
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
