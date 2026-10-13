# Kaitsmise märkmed — Tic-Tac-Toe (Android 01, View System / XML)

Lihtsas keeles kirjas, mida see projekt teeb, miks ta nii on ehitatud ja mida õppejõud tõenäoliselt küsib. Kood ja failinimed on inglise keeles, nagu projektis.

---

## 1. Mis projekt see on

- Kodutöö **Android 01**: Tic-Tac-Toe, kasutajaliides puhtalt XML-is (View System), mitte Compose.
- Pakett ja applicationId: `ee.taltech.tictactoe` (kursuse nõue `ee.taltech.<projekt>`).
- Kaks git-haru:
  - `assignment-01-no-bonuses` — põhinõuded.
  - `assignment-01-with-bonuses` — sama + neli boonust (minimax, suvaline laua suurus, undo, laua pööramine).
- minSdk 26, targetSdk 37. Lisateeke ei ole: ainult Android Studio malli omad (AppCompat, ConstraintLayout, Material, core-ktx) + JUnit ja Espresso testideks.

## 2. Failide kaart — mis fail mida teeb

| Fail | Mida teeb |
|---|---|
| `TicTacToeGame.kt` | **Mängureeglid.** Puhas Kotlin, mitte ühtegi `android.*` importi. Laud, kelle käik, käigu tegemine, võidu/viigi tuvastus, juhuslik käik. Boonusharus ka laua suurus, võiduks vajalik rida, käikude ajalugu ja undo. |
| `MinimaxAi.kt` (boonus) | Minimax + alpha-beta AI. Samuti puhas Kotlin. |
| `Statistics.kt` | Statistika numbrid ühes `data class`-is + keskmise arvutus. |
| `StatisticsStorage.kt` | Statistika salvestamine ja lugemine **SharedPreferences**-ist. Ainus koht, mis seda faili puudutab. |
| `MainActivity.kt` | Mänguekraan: leiab vaated üles, reageerib nuppudele, joonistab mängu seisu, salvestab/taastab seisu pööramisel. |
| `StatsActivity.kt` | Statistika ekraan: näitab numbreid, Reset, Back to game. |
| `res/layout/activity_main.xml` | **Portree**: info-osa üleval, laud all (50/50 Guideline + kaks `<include>`). |
| `res/layout-land/activity_main.xml` | **Landscape**: info vasakul, laud paremal. Sama id-d, Android valib faili ise orientatsiooni järgi. |
| `res/layout/game_info.xml` | Lüliti "Play against AI", raadionupud "X/O starts", seisu tekst, Chronometer, nupud. |
| `res/layout/game_board.xml` | Ruudukujuline laud. Põhiharus 9 nuppu chain'idega; boonusharus tühi `GridLayout`, mille kood täidab. |
| `res/layout/cell_button.xml` (boonus) | Ühe lahtri mall, millest kood teeb koopiad. |
| `res/layout/dialog_board_settings.xml` (boonus) | Laua seadete dialoogi sisu (NumberPicker × 3: read, veerud, võidurida). |
| `res/layout/activity_stats.xml` | Statistika ekraani paigutus. |
| `res/values/strings.xml` | Kõik tekstid + SharedPreferences võtmed. Koodis ega layoutis pole ühtegi kõva teksti. |
| `res/values/colors.xml` + `res/values-night/colors.xml` | Laua värvid heledas ja pimedas teemas. |
| `res/values/styles.xml` | `GameButton` stiil — lahtri nupu välimus ühes kohas. |
| `res/values/dimens.xml` | Mõõdud (`board_margin`, `cell_text_size`, `info_spacing`, `board_gap`). |
| `src/test/...` | JUnit testid reeglitele, AI-le ja statistikale (jooksevad arvutis, ilma telefonita). |
| `src/androidTest/...` | Espresso testid ekraanile ja SharedPreferences salvestusele (jooksevad emulaatoris). |

## 3. Kuidas üks käik läbi süsteemi liigub

1. Kasutaja puudutab lahtrit. XML-is on nupul `android:onClick="buttonCellClicked"`, seega Android kutsub `MainActivity.buttonCellClicked(view)`.
2. Activity leiab, mitmes nupp see listis `cellButtons` on (`indexOf`), ja arvutab rea ja veeru: `rida = indeks / veergudeArv`, `veerg = indeks % veergudeArv`.
3. Activity ütleb mängule `game.makeMove(rida, veerg)`. Mäng ise otsustab, kas käik on lubatud, kelle kord on järgmisena ja kas keegi võitis.
4. Activity kutsub `renderGame()`: käib kõik nupud üle ja paneb igaühele teksti (X / O / tühi) ja tausta (võidulahter roheline), uuendab seisu teksti ja taimerit.
5. Kui mäng lõppes, salvestatakse tulemus samas kohas statistikasse (`statisticsStorage.recordGame`).
6. AI-režiimis planeeritakse AI käik 400 ms hiljem (`Handler.postDelayed`).

Põhimõte: **Activity ei tea reegleid, mäng ei tea nuppe.** Activity ainult tõlgib puudutused mängu keelde ja mängu seisu ekraani keelde.

---

## 4. Tõenäolised küsimused ja vastused

### Paigutus (XML)

**Miks on `activity_main.xml` kahes kaustas?**
`layout/` on portree, `layout-land/` landscape. Android valib ressursi kvalifikaatori järgi ise. Mõlemas on samad id-d (`main`) ja samad `<include>`-id, ainult Guideline on ühes horisontaalne ja teises vertikaalne.

**Kuidas laud alati ruudukujuline on?**
Laua konteineril on `app:layout_constraintDimensionRatio="1:1"` ja mõlemad mõõdud `0dp` (match constraints). ConstraintLayout arvutab suurima ruudu, mis alale ära mahub.

**Miks laua ümber musta raami ei ole?**
Jooned on konteineri **taust** (`board_lines` värv), mis paistab lahtrite vahelt läbi. Lahtrid ulatuvad konteineri servani, nii et serval tausta ei paista. Põhiharus: igas reas horisontaalne chain `spread_inside` (esimene ja viimane serva vastas, vahed ainult vahel), laius `layout_constraintWidth_percent="0.32"` (3 × 0,32 = 0,96, kaks 2 % vahet). Boonusharus: GridLayout, kus margin on ainult lahtri ülemisel ja vasakul küljel, välja arvatud esimesel real ja veerus.

**Miks `GameButton` stiilis on `insetTop=0dp`, `cornerRadius=0dp`?**
Material nupul on vaikimisi 6 dp läbipaistvad servad ja ümarad nurgad. Need jätaksid ebaühtlased augud. Nullid teevad nupust täpse ruudu.

**Mis on `<include>` ja miks sellel on `layout_width` ja `layout_height`?**
`<include>` paneb teise layout-faili sellesse kohta. Kui includel on laius ja kõrgus, kirjutab ta kaasa tulnud juurelemendi omad üle — mõlemad peavad olema koos antud, muidu ignoreeritakse.

**Miks `dp` ja `sp`?**
`dp` on tihedusest sõltumatu mõõt, et asjad oleks eri ekraanidel sama suured. `sp` on tekstile, et kasutaja fondisuuruse seadistus toimiks.

**Miks info-osa on `ScrollView` sees (boonusharus)?**
Boonusharus on rohkem nuppe ja landscape'is on vasak pool madal. ScrollView laseb vajadusel kerida. `fillViewport="true"` venitab sisu vähemalt ekraani kõrguseks.

### Pööramine ja seisu säilitamine

**Mis juhtub, kui telefoni pööratakse?**
Android hävitab Activity (`onDestroy`) ja loob uue (`onCreate`) teise layoutiga. Kõik muutujad kaovad. Sellepärast salvestan seisu `onSaveInstanceState(outState: Bundle)`-is ja loen `onCreate`-is `savedInstanceState`-ist tagasi.

**Mida täpselt salvestad?**
Põhiharus: laud 9-täheline string (`"X.O......"`), kelle käik, kas vs AI, taimeri alguse aeg, lõppenud mängu kestus, kas "New game" dialoog oli lahti. Boonusharus laua stringi asemel **käikude ajalugu** (int massiiv) + laua suurus ja alustaja — taastamisel mängitakse käigud uuesti läbi (`TicTacToeGame.fromMoves`), siis on ka undo ajalugu olemas.

**Miks võitjat ja võidulahtreid ei salvesta?**
Taastamisel arvutatakse need laua seisust uuesti. Vähem asju, mis võiks lahku minna.

**Miks lüliti ja raadionupud ei ole Bundle'is?**
Vaated, millel on `android:id`, salvestavad oma seisu ise. Android teeb seda automaatselt.

**Miks mitte `android:configChanges="orientation"` manifestis?**
Siis Activity't ei looda uuesti ja landscape layout ei lähe kasutusse. Kursuse materjal ütleb otse: seda ei tehta.

**Mis on `super.onCreate(savedInstanceState)` ja miks esimesena?**
Kutsub vanemklassi (AppCompatActivity) oma töö — ilma selleta Activity ei tööta. Elutsükli meetodites kutsutakse super alati esimesena.

### Nupud ja vaated

**`android:onClick` vs `setOnClickListener` — miks XML-is?**
Nii õppisin esimeses projektis. XML-is `android:onClick="buttonNewGameClicked"` → Activity's `fun buttonNewGameClicked(view: View)`. Meetod peab olema `public`, võtma ühe `View` argumendi ja nimi peab täpselt klappima, muidu crash klikkimisel.

**Mis on `lateinit var`?**
Muutuja, mille väärtus pannakse hiljem (`onCreate`-is `findViewById`-ga), mitte kohe. Ilma `lateinit`-ita peaks ta olema nullable (`Button?`) ja iga kasutus vajaks `?.`. Kui kasutad enne initsialiseerimist, tuleb selge viga.

**Miks lahtrid on listis `cellButtons`?**
Et ühe tsükliga kõiki üle käia (`renderGame`) ja klikitud nupu asukohta leida (`indexOf`). Üheksa eraldi muutujat tähendaks üheksa korda sama koodi.

### Mänguloogika

**Miks reeglid on eraldi klassis, mitte Activity's?**
1) Testitav arvutis ilma telefonita (JUnit, sekund). 2) Activity jääb lühikeseks. 3) Sama klassi saab Compose kodutöös muutmata kasutada.

**Kuidas võitu kontrollitakse?**
Põhiharus: 8 rida (3 rida, 3 veergu, 2 diagonaali) on listis `ALL_LINES`; pärast käiku vaatan, kas mõnes reas on kolm sama sümbolit. Boonusharus: vaatan ainult **viimase käigu lahtrist** läbi minevat nelja suunda (horisontaal, vertikaal, kaks diagonaali) ja loen mõlemale poole, mitu sama sümbolit järjest on. Kui ≥ `winLength`, on võit. Nii töötab iga laua suurusega.

**Mis on `enum class Player { X, O }`?**
Kindlate väärtuste tüüp — mängija saab olla ainult X või O, ei mingit "Y". `opponent()` annab vastase. Tühi lahter on `null` (`Player?`).

**Mis on `data class Cell(val row: Int, val column: Int)`?**
Väike klass, mis hoiab rida ja veergu koos. `data class` annab tasuta `equals`, nii et `Cell(0,1) == Cell(0,1)` on tõene ja `cell in game.winningCells` töötab.

**Mis on `var status ... private set`?**
Väljast saab lugeda (`game.status`), aga muuta saab ainult klass ise. Activity ei saa kogemata võitjat "ära muuta".

**Mis on `companion object` ja `const val`?**
Klassi-tasemel asjad, mida ei pea objekti kaudu kutsuma: konstandid (`BOARD_SIZE`), abifunktsioonid (`fromMoves`). `const val` on kompileerimisaegne konstant.

**Kuidas juhuslik AI töötab?**
`emptyCells().random(random)` — tühjade lahtrite list ja sellest juhuslik. `Random` antakse konstruktoris ette, et testides saaks seemnega (`Random(42)`) sama tulemuse korrata. **Boonusharus** mängib AI alati minimaxiga; `makeRandomMove()` jäi mootorisse ainult testide jaoks (minimaxi vastane testides).

### AI viivitus

**Miks AI käik on 400 ms hiljem ja miks mitte `Thread.sleep`?**
Kohene vastus nägi välja, nagu oleks kaks lahtrit korraga vajutatud. `Thread.sleep` peataks **UI lõime**: ekraani ei joonistataks enne, kui sleep läbi, X ja O ilmuksid ikka koos ja äpp oleks külmunud. `Handler(Looper.getMainLooper()).postDelayed({ makeAiMove() }, 400)` ütleb: jooksuta see 400 ms pärast samal UI lõimel. Vahepeal on lõim vaba.

**Mis siis vahepeal juhtuda võib?**
Kolm kaitset: 1) kui on AI kord, ignoreerin puudutusi (`isAiTurn()`); 2) "New game" ja `onDestroy` tühistavad ootel käigu (`removeCallbacksAndMessages(null)`); 3) pööramise järel, kui on ikka AI kord, planeerin käigu uuesti — muidu jääks mäng seisma.

### Taimer

**Kuidas mängu aeg töötab?**
XML-i `Chronometer` vidin tiksub ise. Esimesel käigul `base = SystemClock.elapsedRealtime()` ja `start()`; mängu lõpus `stop()`. Enne esimest käiku `base = praegu` + `stop()` → näitab 00:00.

**Miks `SystemClock.elapsedRealtime()` ja mitte `System.currentTimeMillis()`?**
`elapsedRealtime` on aeg alates seadme käivitamisest — ei hüppa, kui kell või ajavöönd muutub, ja Chronometer tahab just seda. Kestuste jaoks õige kell.

**Kuidas taimer pööramist üle elab?**
Salvestan alguse aja (Long) Bundle'isse ja panen taastamisel uuesti `base`-iks. Lõppenud mängul salvestan kestuse ja panen `base = praegu − kestus` + `stop()`, siis näitab õiget külmutatud aega.

### Statistika ja SharedPreferences

**Kuidas statistika salvestatakse?**
`context.getSharedPreferences(getString(R.string.preference_file_statistics), Context.MODE_PRIVATE)` — fail `tictactoe_statistics`, mida näeb ainult see äpp. Võtmed (`games_played`, `wins_x`, …) on `strings.xml`-is, et nimi oleks ühes kohas. Kirjutamine: `with(sharedPreferences.edit()) { putInt(...); putLong(...); apply() }`.

**`apply()` vs `commit()`?**
`apply()` salvestab taustal ja ei blokeeri UI lõime; `commit()` ootab ketta järel. Loengu näide kasutab `apply()`.

**Miks keskmist aega ei salvestata?**
Salvestan kogukestuse (`total_duration_ms`) ja mängude arvu, keskmine = jagatis. Ei teki ümardamisvigu ja Reset nullib lihtsalt kõik.

**Millal salvestatakse?**
Samas funktsioonis, kus lõpetav käik tehti (`onMoveMade`). Pööramisel seda ei kutsuta, seega topelt ei loeta. Katkestatud mäng (New game keset mängu) ei lähe statistikasse.

**Kuidas statistika ekraan andmed saab?**
Avab ise `StatisticsStorage(this).load()` oma `onResume()`-is. Intentiga ei saadeta midagi — fail on ainus tõde, Reset'i järel on numbrid kohe õiged.

### Teine Activity

**Kuidas statistika ekraan avatakse?**
`startActivity(Intent(this, StatsActivity::class.java))` — explicit intent. Manifestis `<activity android:name=".StatsActivity" android:exported="false" />`, sest teised äpid ei pea seda avama. "Back to game" teeb `finish()`, mis sulgeb selle Activity ja mänguekraan tuleb tagasi täpselt sellises seisus nagu oli.

### Dialoogid

**Kuidas "New game" kinnitus töötab?**
`AlertDialog.Builder(this).setTitle(...).setPositiveButton(...) { _, _ -> startNewGame() }.setNegativeButton(..., null).show()`. Näidatakse ainult siis, kui mäng on pooleli ja vähemalt üks käik tehtud.

**Miks dialoog pööramist üle elab?**
Boolean `isNewGameDialogShowing` on Bundle'is; `onCreate` näitab dialoogi uuesti. `onDestroy`-s `newGameDialog?.dismiss()`, muidu tuleb "window leaked" viga.

### Teema

**Kuidas dark mode töötab?**
Teema on `Theme.Material3.DayNight.NoActionBar` — nupud, tekstid, dialoogid vahetuvad ise. Enda värvidel (`board_lines`, `cell_background`, `cell_winning`, `cell_text`) on `values-night/colors.xml`-is teine väärtus. Koodis ei ole ühtegi hex-värvi: `ContextCompat.getColor(this, R.color.cell_winning)`.

**Miks `setBackgroundColor` ja mitte eraldi drawable?**
Lihtsaim viis ühe nupu tausta muuta; Material nupp suunab selle oma tausta tinti. Üks rida, mida on lihtne selgitada.

### Edge-to-edge kood `onCreate`-is

**Mis see `ViewCompat.setOnApplyWindowInsetsListener(...)` blokk on?**
Android Studio malli kood. `enableEdgeToEdge()` lubab sisu joonistada staatus- ja navigeerimisriba alla; listener loeb süsteemiribade suurused ja paneb juurvaatele sama suure padding'u, et nupud ei jääks riba alla.

### Testid

**Mis teste on?**
- JUnit (arvutis): `TicTacToeGameTest` (iga võidurida, viik, keelatud käik, taastamine, undo), `MinimaxAiTest`, `StatisticsTest`.
- Espresso (emulaatoris): `GameScreenTest` (võit, dialoog, pööramise simulatsioon `scenario.recreate()`, statistika ekraan, undo, rotate), `StatisticsStorageTest` (päris SharedPreferences).
- Käsud: `./gradlew test`, `./gradlew connectedAndroidTest`, `./gradlew lint`.

**Miks `inRoot(isDialog())` Espresso testides?**
Espresso ei pea dialoogi akent "juureks" enne, kui see on fookuse saanud. Kohe pärast recreate't on vahel aknal fookus veel puudu ja test jääb ootama. `inRoot(isDialog())` ütleb: otsi dialoogi aknast.

---

## 5. Boonused (ainult `assignment-01-with-bonuses` harus)

### Suvaline laua suurus ja võidurida

- `TicTacToeGame(rowCount, columnCount, winLength, startingPlayer)`; 3…10, võidurida ≤ suurem külg.
- Laud ehitatakse koodis: `buildBoard()` tühjendab `GridLayout`-i, paneb `rowCount`/`columnCount`, ja `layoutInflater.inflate(R.layout.cell_button, ...)` teeb iga lahtri. `GridLayout.spec(index, 1f)` = võrdne kaal igal real ja veerul → lahtrid võrdsed. Mitteruudukujulisel laual `dimensionRatio = "columns:rows"`, laud on ristkülik ruudu sees.
- Tekst skaleerub: `cell_text_size × 3 / suuremKülg`.
- Seaded: dialoog kolme `NumberPicker`-iga; kehtivad järgmisest mängust (või kohe, kui laud on tühi).

### Undo

- Mäng hoiab käikude ajalugu (`moves`). `undoMove()` võtab viimase, nullib lahtri, annab käigu tagasi, paneb staatuse `IN_PROGRESS`.
- AI-režiimis võetakse tagasi kaks käiku (AI oma ja enda oma), et jääks inimese kord.
- Lõppenud mängus undo ei tööta (nupp disabled), sest tulemus on juba statistikas.

### Laua pööramine

- `gridLayoutBoard.animate().rotation(boardRotationDegrees)` — pööratakse ainult laua vaadet, ülejäänud ekraan jääb paika. Puudutused lähevad õigesse lahtrisse, sest Android arvestab vaate pööret ise.
- Iga vajutus lisab sihtnurgale 90°. Animeeritakse **absoluutse** nurgani (`rotation`, mitte `rotationBy`), seega kiire topeltvajutus lõpeb 180°, mitte suvalise nurga peal.
- `0f`, `90f`: Float, sest `View.rotation` on Float. Kotlin ei teisenda Int → Float ise.

### Minimax + alpha-beta

- Idee: proovin käiku, lasen vastasel parimat vastata, mina jälle parimat jne, ja valin käigu, mille lõpptulemus mulle parim. `makeMove` → rekursioon → `undoMove` — ei kopeeri lauda.
- Skoor: võit = suur pluss (kiirem võit parem: `WIN_SCORE − depth`), kaotus = suur miinus, viik 0.
- **Alpha-beta**: alpha = parim, mille mina kindlalt saan; beta = parim, mille vastane kindlalt saab. Kui need ristuvad (`beta <= alpha`), ei ole mõtet haru lõpuni vaadata → `break`. Tulemus sama, aga palju vähem tööd.
- 3×3 vaadatakse mäng lõpuni (võitmatu). Suurel laual sügavust piiratakse (16 lahtrit: 6; 36: 4; 64: 3; muidu 2) ja lõpus hinnatakse seisu **hindamisfunktsiooniga**: loen kõik võimalikud võiduaknad (`winLength` järjestikust lahtrit), kus on ainult ühe poole sümbolid; 1/2/3/4 sümbolit = 1/10/100/1000 punkti, enda omad pluss, vastase omad miinus. Nii ehitab AI ridu ja blokeerib varakult.
- Suurel laual vaadatakse ainult lahtreid, mis puutuvad kokku mõne olemasoleva nupuga (tühjal laual keskpunkt) — kaugel servas käigud ei ole kunagi kasulikud ja hargnemine jääb väikeseks. Mõõdetud: halvim juhtum ~30 ms arvutis.
- AI mängib boonusharus **alati** minimaxiga — valikut "juhuslik / minimax" ei ole. 3×3 laual on ta võitmatu, suurel laual tugev, aga piiratud sügavusega.

---

## 6. Teadaolevad piirangud (ütle ise ära, enne kui küsitakse)

- Laua seaded, režiim ja alustaja ei säili äpi sulgemisel (ainult pööramisel). Assignment nõudis püsivat salvestust ainult statistikale.
- Kui süsteem tapab äpi taustal, läheb pooleliolev mäng kaduma; lõppenud mängude statistika on alati alles.
- Minimax jookseb UI lõimel. Suurel laual on see mõõdetult kiire (< 100 ms), seetõttu ei teinud taustalõime — oleks lisanud keerukust ilma nähtava kasuta.
- Espresso testides on kaks `Thread.sleep`-i (AI viivitus, pöörde animatsioon) — testis on see lubatud lihtsus.

## 7. Käsud

```bash
./gradlew assembleDebug          # build
./gradlew test                   # JUnit testid (arvutis)
./gradlew connectedAndroidTest   # Espresso testid (emulaator peab käima)
./gradlew lint                   # Android Lint
adb shell cmd uimode night yes   # pime teema emulaatoris (no = tagasi)
```
