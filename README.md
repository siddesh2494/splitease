# SplitEase — Android expense-splitting app

Kotlin · Jetpack Compose (Material 3) · Room · Hilt · Navigation Compose (type-safe) · compileSdk/targetSdk 36 (Android 16)

> **Status:** the domain logic (debt simplification, split engine) is covered by unit tests in
> `app/src/test`. The full Android project was written without access to the Android SDK, so it has
> **not been compiled yet** — expect to fix a few small compile/version issues on first sync
> (library versions in `gradle/libs.versions.toml` should be bumped to the latest stable).

## What's new in 1.1.0
- **Delete a group** (⋮ menu on a group card, or ⋮ → Delete group inside a group), with confirmation.
- **Edit expenses:** tap any expense. Swipe left to delete, with Undo.
- **Edit group & members:** ⋮ → Edit group & members. Rename the group, rename / add / remove people.
  Someone who is still in an expense or payment can't be removed until those are edited.
- **Add a person while adding or editing an expense**, and untick people to remove them from it.
- **New design:** Plus Jakarta Sans font, gradient hero cards, design tokens. See `DESIGN.md`.
- **Fixed debug signing key** (`app/debug.keystore`) so future updates install over the old app.

## 1. Project setup (step by step)

1. Install **Android Studio** (current stable), **JDK 17**, and in SDK Manager install
   **Android SDK Platform 36** + latest **Build-Tools**.
2. Open this folder in Android Studio → *Open*. Let Gradle sync. If there is no `gradlew`, run
   `gradle wrapper --gradle-version 8.14.3` once (or let Studio create it).
3. Run on an emulator/device (minSdk 26) with ▶.

### Package layout
```
com.splitease.app
├── di/            Hilt modules (DatabaseModule)
├── data/
│   ├── local/     Room: entity/Entities.kt, dao/Daos.kt, AppDatabase.kt
│   └── repository/SplitRepository.kt   offline-first single source of truth
├── domain/
│   ├── model/     Group, Expense, Split, Settlement, Transfer …
│   └── usecase/   DebtSimplifier, SplitCalculator, BalanceCalculator
├── notifications/ local reminder notifications
└── ui/            theme, navigation, home, group (dashboard + add-expense), settle
```

## 2. Where each requirement lives

| Requirement | File |
|---|---|
| Minimal-transfer debt simplification | `domain/usecase/DebtSimplifier.kt` |
| Equal / exact / % / shares splits (no lost cents) | `domain/usecase/SplitCalculator.kt` |
| Multi-currency + manual rate overrides (stored offline) | `ExchangeRateEntity`, `SplitRepository.rateFor/setManualRate`, rate field in `AddExpenseSheet` |
| Recurring expenses | `Recurrence` + `SplitRepository.materializeRecurring` (run at app start) |
| Activity feed / audit log | `ActivityEntity`, shown in the Activity tab |
| Settlements (cash / UPI / bank) | `SettleUpScreen`, `SplitRepository.recordSettlement` |
| Invite via link/code | Share button in dashboard; deep links in `AndroidManifest.xml` |
| Edge-to-edge | `enableEdgeToEdge()` + Scaffold/inset padding |
| Predictive back | `android:enableOnBackInvokedCallback="true"` + NavHost |
| Shared-element transition | `SharedTransitionLayout` in `AppNavHost` (group title) |
| Haptics | `LocalHapticFeedback` on FAB, Record, Save |

### About "minimal transactions"
Truly minimal settlement is NP-hard. `simplifyOptimal` partitions members into the maximum number of
zero-sum subsets (exact, up to 16 non-zero balances) and runs a greedy matcher inside each; larger
groups fall back to greedy, which never exceeds n−1 transfers.

## 3. Build a release APK

```bash
# Unsigned release APK (output: app/build/outputs/apk/release/app-release-unsigned.apk)
./gradlew assembleRelease

# Debug APK (auto-signed with the debug key; installable immediately)
./gradlew assembleDebug
```

### Signed release APK
```bash
keytool -genkeypair -v -keystore release.jks -alias splitease \
        -keyalg RSA -keysize 2048 -validity 10000
cp keystore.properties.example keystore.properties   # then fill in passwords
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```
Verify: `apksigner verify --verbose app/build/outputs/apk/release/app-release.apk`
Install: `adb install -r app/build/outputs/apk/release/app-release.apk`

Never commit `release.jks` or `keystore.properties` (already in `.gitignore`).

## 4. Not included yet (honest scope notes)
- **Cloud sync / true shared groups:** data is local only. Invite codes resolve only to groups that
  already exist on the device. Real multi-device sharing needs a backend (e.g. Firebase/Supabase) —
  `SplitRepository` is the single place to add it.
- **Live exchange rates:** rates are manual/cached; add a fetch worker if you want auto-updates.
- **Edit expense UI**, member removal, notification scheduling UI (`Notifier` is ready to call).
- **Launcher icon** is a simple placeholder vector.
