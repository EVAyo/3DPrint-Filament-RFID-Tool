# AGENTS.md

This file gives Codex/agent guidance for this repository. It is based on the
checked-in Gradle files, Android manifest, Kotlin source, assets, tests, and
the local workspace state observed when this file was generated. Do not treat
uncertain items as project policy without checking them again.

## Project Purpose

BambuRfidReader / 3DPrint-Filament-RFID-Tool is an Android app for reading,
cloning, writing, sharing, and managing 3D printer filament RFID tags with a
phone. The app code supports Bambu Lab, Creality, and Snapmaker reader modes.

Confirmed platform constraints:
- Android app module: `:app`
- Package/application id: `com.m0h31h31.bamburfidreader`
- Minimum SDK: 28
- Target SDK: 36
- Compile SDK: 36
- Manifest requires NFC hardware and uses `android.nfc.action.TECH_DISCOVERED`
- The NFC implementation uses `android.nfc.tech.MifareClassic`

## Architecture

The main Android flow is:

```text
NFC Tag
  -> MainActivity.NfcAdapter.ReaderCallback
  -> brand-specific pending task handling or read path
  -> NfcTagReader.readRaw() for Bambu/Mifare Classic raw reads
  -> NfcTagProcessor.parseAndPersist() for Bambu parsing and inventory writes
  -> FilamentDbHelper SQLite persistence
  -> Compose screens via AppNavigation
```

Key components:
- `app/src/main/java/com/m0h31h31/bamburfidreader/MainActivity.kt`
  - Central app state, NFC callback, brand dispatch, write/verify workflows,
    database helper, import/export, update download/install, logging, and many
    data classes.
- `app/src/main/java/com/m0h31h31/bamburfidreader/NfcTagReader.kt`
  - Bambu/Mifare Classic raw sector reads, per-UID HKDF-SHA256 key derivation,
    retry/reconnect behavior, and `RawTagReadResult`.
- `app/src/main/java/com/m0h31h31/bamburfidreader/NfcTagProcessor.kt`
  - Bambu tag block parsing, display model construction, catalog matching, and
    inventory persistence.
- `app/src/main/java/com/m0h31h31/bamburfidreader/ui/navigation/AppNavigation.kt`
  - Compose navigation and tab visibility gates.
- `app/src/main/java/com/m0h31h31/bamburfidreader/ui/screens/`
  - Compose screen implementations for reader, inventory, data, Bambu tags,
    Snapmaker tags, Creality, misc settings/tools, and NDEF writing.
- `app/src/main/java/com/m0h31h31/bamburfidreader/ui/components/`
  - Shared UI pieces such as `NeuPanel`, `NeuButton`, `ColorSwatch`, and
    `InfoLine`.
- `app/src/main/java/com/m0h31h31/bamburfidreader/ui/theme/`
  - Compose theme, color palettes, and UI style enums.
- `app/src/main/java/com/m0h31h31/bamburfidreader/utils/`
  - Remote config, HTTP helpers, analytics/update/reporting, and tag sharing.
- `app/src/main/java/com/m0h31h31/bamburfidreader/util/`
  - Small utility helpers for colors and TTS settings.

Uncertain/local-only:
- A `server/` directory exists in the local workspace and contains a FastAPI
  service, but `.gitignore` excludes `server/*` and `git ls-files` does not list
  it. Treat it as local support code unless it is intentionally added to Git.

## Folder Structure

```text
.
|-- app/
|   |-- build.gradle.kts
|   |-- proguard-rules.pro
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- assets/
|       |   |   |-- AppConfig.json
|       |   |   |-- creality_material_list.json
|       |   |   |-- filaments_color_codes.json
|       |   |   |-- filaments_type_mapping.json
|       |   |   `-- rfid_data.zip
|       |   |-- java/com/m0h31h31/bamburfidreader/
|       |   |-- res/
|       |   `-- ic_launcher-playstore.png
|       |-- test/
|       `-- androidTest/
|-- gradle/
|   |-- libs.versions.toml
|   `-- wrapper/
|-- img/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- README.md
`-- user documentation markdown file with a Chinese filename
```

## Build And Test Commands

Use the Gradle wrapper from the repository root.

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
./gradlew clean
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
```

Current test coverage is minimal: the checked-in unit and instrumented tests are
the default example tests.

## Coding Conventions

Observed conventions:
- Kotlin + Jetpack Compose + Material 3 are used for the app UI.
- Java/Kotlin compatibility target is JVM 11.
- Gradle dependencies are mostly version-catalog based in
  `gradle/libs.versions.toml`, with a few direct dependencies in
  `app/build.gradle.kts`.
- Most app-wide mutable UI state currently lives in `MainActivity` as
  `mutableStateOf(...)` and is passed to composables.
- Constants are mostly file-private `private const val` or `private val` near
  the top of the Kotlin file.
- Use the top-level `logDebug(message)` function for app diagnostics; it writes
  both to Android `Log.d` and `LogCollector`.
- Network work uses coroutines on `Dispatchers.IO` and `HttpURLConnection` in
  `NetworkUtils`; no Retrofit/OkHttp wrapper is present.
- JSON parsing is done with `org.json`.
- Database schema and migrations are defined in `FilamentDbHelper` inside
  `MainActivity.kt`.
- Feature gates and UI preferences use `SharedPreferences`.
- Prefer preserving existing style and placement. This codebase is centralized
  in `MainActivity`, so avoid introducing broad architecture changes for small
  fixes.

Encoding note:
- Some source comments/documentation may render as mojibake in a default
  terminal. Do not rewrite large comment blocks just to normalize encoding.

Localization rule:
- 新增用户可见文本必须加入 Android 多语言资源，不要在 Kotlin/Compose
  代码中直接写死可见文案。
- 新增字符串至少同步更新 `app/src/main/res/values/strings.xml`,
  `app/src/main/res/values-en/strings.xml`, and
  `app/src/main/res/values-zh-rCN/strings.xml`; if the Chinese fallback file
  `app/src/main/res/values-zh/strings.xml` exists for the touched feature,
  keep it in sync as well.
- Kotlin/Compose should read localized text through `stringResource(...)`,
  `getString(...)`, or this app's helper wrappers such as `uiString(...)`.

## Business Rules Discovered From Code

NFC and read behavior:
- Bambu raw reads require `MifareClassic`; unsupported tags return
  `RawTagReadResult.Failure(MIFARE_UNSUPPORTED)`.
- Bambu sector keys are derived from the tag UID with HKDF-SHA256 using fixed
  salt and `"RFID-A\u0000"` / `"RFID-B\u0000"` info strings.
- Derived Bambu keys are cached by UID in `NfcTagReader`.
- Default Bambu read scope is sectors 0 through 4; full sector reads are used
  when `readAllSectors` is enabled or when auto-share forces a full read.
- `NfcTagReader` should remain pure I/O; parsing and database writes belong in
  `NfcTagProcessor` or higher layers.

Bambu NFC core rules:
- Bambu core MIFARE operations must go through the unified Bambu NFC operation
  layer in `app/src/main/java/com/m0h31h31/bamburfidreader/nfc/`.
- Bambu reads authenticate sectors with the UID-derived KeyA. Do not use
  UID-derived KeyB as a normal Bambu read fallback.
- Bambu formatting must treat only UID-derived KeyB authentication as permission
  to reset a Bambu trailer. KeyA success is not enough for trailer reset.
- Bambu formatting first writes the trailer with derived KeyA + `FF078069` +
  derived KeyB, then uses KeyB-capable access to change KeyA and KeyB to the
  default `FF FF FF FF FF FF` values.
- Bambu write-from-dump expects the target sectors to authenticate with the
  default `FF` key and writes through that default key.
- Bambu C-card modify/rewrite must format first, then write the dump through the
  same default-FF write path.
- Android stale tag errors such as `Tag is out of date` should abort the current
  NFC operation with a re-tap instruction instead of being retried as ordinary
  auth failures.
- NFC compatibility presets include a mode-specific post-key-derivation delay
  before MIFARE authentication. Keep this delay in mind when tuning old-phone
  read/write stability.
- Stable NFC compatibility mode should stay close to MifareClassicTool-style
  authentication behavior: more retries and less reconnecting after ordinary
  authentication false results, while preserving stale-tag reconnect handling.

Android NFC stability rules verified in local testing:
- The current Android `MifareClassic` read/write path is considered stable.
  Do not refactor or simplify the NFC authentication, reconnect, delay, or
  format logic unless testing on real Android phones shows a concrete issue.
- Keep Bambu read/write/format/verify operations on the unified NFC operator.
  Avoid reintroducing separate block read/write loops in `MainActivity` or
  `NfcTagReader`.
- Keep Bambu read authentication strict: UID-derived KeyA is the normal read
  key. UID-derived KeyB is for trailer reset/format logic, not a normal read
  fallback.
- Keep the mode-specific `postKeyDerivationDelayMs` before MIFARE
  authentication. This delay is part of the old-phone compatibility fix.
- Keep Stable mode's MifareClassicTool-like behavior:
  `reconnectAfterFailedAuth = false`, more retries, and less reconnect churn
  after ordinary auth false results. Do not change it back to reconnect after
  every failed auth without real-device evidence.
- Config-page "format tag" must auto-detect the card family and use matching
  keys. The supported detection families are Bambu, Snapmaker/快造,
  Creality/创想三维, and default `FF`/blank cards.
- Format detection must not classify a tag as default `FF` only because sector 0
  authenticates with `FF`. Partially formatted cards can have sector 0 reset
  while later sectors still use brand keys. Detection should check Bambu and
  Snapmaker sectors 0/1 and Creality sector 1 before falling back to `FF`.
- Snapmaker/快造 formatting must use the two-step trailer reset:
  derived KeyB auth -> write derived KeyA + `FF078069` + derived KeyB -> then
  authenticate again and write default `FF` KeyA/KeyB. Do not replace this with
  a one-step default trailer write.
- Creality/创想三维 formatting uses `deriveCrealityKeyA(uid)` plus `FF`
  fallback where applicable, then resets sectors to the default `FF` trailer.
- Non-Bambu format should preserve block 0, reset trailers to
  `FF FF FF FF FF FF FF 07 80 69 FF FF FF FF FF FF`, clear data blocks, and
  verify with `FF`.
- NFC debug logs for key derivation, brand detection, trailer reset stages, and
  stale tag handling are intentional. Do not remove them while real-device NFC
  compatibility remains an active concern.

Bambu parsing and inventory:
- Bambu parsing currently uses blocks 0..7 plus block 12 and block 16.
- Parsed fields include material variant/id, base filament type, detailed
  filament type, RGBA color, spool weight, diameter, drying settings, bed/nozzle
  temperatures, production date, and optional block 16 multi-color data.
- Tray UID is read from block 9 and is the key used for inventory persistence.
- New inventory uses a default remaining percent of 100.
- If remaining grams are missing/zero and total spool weight is available,
  remaining grams are initialized to total weight.
- Remaining percent is rounded to one decimal place in `upsertTrayRemaining`.
- Display data prefers catalog matches from SQLite by `fila_id` and color data;
  it falls back to parsed tag fields when no catalog match exists.
- Multi-color catalog matching normalizes colors and compares sorted lists, so
  color order is not significant.

Data/config:
- Seed assets are `filaments_color_codes.json`,
  `filaments_type_mapping.json`, `creality_material_list.json`, and
  `AppConfig.json`.
- Runtime config lookup reads external app files first, then bundled assets.
- Remote config fetch uses Gitee primary URLs and GitHub backup URLs.
- Updating filament/color or type-mapping config triggers filament database
  re-sync; updating Creality material config triggers Creality database re-sync.
- SQLite database name is `filaments.db`; schema version is 23.
- Existing schema tables include `filaments`, `filament_inventory`,
  `share_tags`, `snapmaker_share_tags`, `creality_materials`,
  `filament_type_mapping`, `meta_v2`, and `anomaly_uids`.
- Any schema change must bump `FILAMENT_DB_VERSION` and add an `onUpgrade`
  migration.

Brand-specific behavior:
- `ReaderBrand` has `BAMBU`, `CREALITY`, and `SNAPMAKER`.
- Bambu tag-library tab, Creality tab, Snapmaker tab, and inventory/data tabs
  are preference-gated.
- Auto-detect can switch brand based on sector 0 detection logic.
- Creality uses AES-based key/data handling and maps configured length codes to
  weights such as `1 KG`, `750 G`, `600 G`, `500 G`, and `250 G`.
- Snapmaker uses its own salts for key derivation and RSA public keys for
  official-tag verification.

Sharing, copy counts, and import/export:
- A raw shared tag is considered complete when UID is nonblank and at least one
  raw block is present.
- Auto share is enabled by default and stores uploaded UIDs in
  `tag_share_prefs` to avoid duplicate uploads.
- Shared raw tag payloads include brand, UID, blocks, derived keys, and device
  id. Trailer blocks are rebuilt with derived KeyA/KeyB before upload.
- `share_tags.copy_count` increments after successful write attempts; verified
  is set only after verification succeeds.
- Formatting/clearing an FUID can reset matching share-tag copy count and
  verified status by tray UID.
- Tag package import/export uses ZIP flows. The app includes bundled
  `rfid_data.zip` and extracts it once using `.bundle_extracted`.
- Tag package ZIP password calculation is
  `SHA-256(install_id:TAG_PACKAGE_KEY)[:16]` in the Android code.

Networking, analytics, and updates:
- `EVENT_API_KEY` and `TAG_PACKAGE_KEY` come from `local.properties` and are
  exposed through `BuildConfig`; missing values become empty strings.
- `AnalyticsReporter` adds `X-API-Key` only when `EVENT_API_KEY` is nonblank.
- Install/launch events include package/version/device metadata and are keyed
  by Android ID.
- The app can report anomaly UIDs, fetch anomaly counts, report UID copy events,
  fetch UID copy counts, save nicknames, and check for updates.
- Update checking expects JSON with fields including `versionCode`,
  `downloadUrl`, `versionName`, `changelog`, and `forceUpdate`.
- APK updates are downloaded with `DownloadManager` and installed through the
  manifest `FileProvider` authority `${applicationId}.update_provider`.

## Deployment Workflow

Confirmed Android release steps from repository files:
- Build debug APK with `./gradlew assembleDebug`.
- Build release APK with `./gradlew assembleRelease`.
- Release minification is currently disabled (`isMinifyEnabled = false`).
- Release version is controlled in `app/build.gradle.kts` via `versionCode` and
  `versionName`.
- Runtime update metadata is fetched from the configured update endpoint
  (default in code: `https://brr.jacki.cn/api/update/latest`).

Uncertain:
- No checked-in CI workflow was found.
- No checked-in Play Store, GitHub Release, or Gitee Release automation was
  found.
- No signing configuration was found in checked-in Gradle files; release
  signing likely depends on local/IDE configuration or external process.
- The local-only `server/` FastAPI code appears to support release upload and
  latest-update endpoints, but because it is ignored and not tracked, do not
  assume it is the canonical deployment path without confirmation.

## Known Constraints

- Requires Android NFC hardware and Mifare Classic support. The manifest marks
  NFC hardware as required.
- Some Android devices can detect NFC tags but cannot fully read/write Mifare
  Classic because of hardware/controller limitations.
- MainActivity is very large and owns many responsibilities; small changes
  should be scoped carefully to avoid regressions.
- NFC operations are stateful. `readingInProgress` prevents concurrent reader
  callback handling; preserve this guard.
- Write/format/verify operations are controlled by pending state variables.
  Avoid starting overlapping NFC tasks.
- Config and catalog data should come from external files or bundled assets, not
  hardcoded replacements.
- Database migrations must preserve user inventory/share data where possible.
- `local.properties` is ignored and is the expected local source for API keys.
- The repository tracks IDE metadata and Kotlin error logs; avoid modifying
  unrelated tracked workspace files unless the task requires it.
