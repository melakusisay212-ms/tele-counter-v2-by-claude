# Tele Expense Counter

Offline Android app that turns Ethiopian telecom SMS (Ethio telecom, Safaricom Ethiopia, eBirr/telebirr) into a clean list of telecom expenses. Everything runs on the phone: rule-based parsing, no backend, no cloud, raw SMS bodies are never stored.

- **Ethiopian calendar first** (Pagume and leap years handled); periods: Today / This Week / This Month / This Year / Custom.
- **Spending is grouped as Voice, Data and SMS** (airtime counts as Voice; mixed bundles and anything else go to Other).
- **One purchase = one expense.** Several SMS about the same top-up (e.g. the three eBirr messages for an ETB 5 top-up) are merged.
- Telebirr-inspired look: white background, black text, green accents.
- First run: *Scan This Month* or *Start From Today*; afterwards new SMS arrive through a receiver, plus a manual *Scan New Messages*.
- Edit, delete, or mark any transaction as "not an expense".

## Layout
- `parser/` – pure Kotlin module: parser, calendar, grouping, analytics, tests.
- `app/` – Jetpack Compose UI, SQLite storage, SMS receiver.
- `data/sms_training.json` – labelled sample messages used by tests.

## Build
Push to GitHub with `settings.gradle.kts` at the repo root. The *Build Debug APK* workflow runs the parser tests, builds the APK and uploads it as the `tele-expense-debug-apk` artifact.
Locally: `gradle :parser:test :app:assembleDebug` (JDK 17, Gradle 8.9).

## Sideloading note
On Android 13+, apps installed outside the Play Store need *App info → ⋮ → Allow restricted settings* before SMS permission can be granted. The app explains this when permission is denied.

## Spending rules (V2)
- **Total = cash only.** Money that really left a bank or telebirr: direct package payments and airtime recharges.
- **Airtime is a balance.** A bundle bought from airtime, or airtime sent to a number, is a label on airtime already paid for. It never adds to the total; it moves part of "Airtime not yet used" into Voice, Data, SMS or Others.
- **Home tiles:** Voice, Data, SMS, plus Combo (mixed bundles) and Others when they have something, plus "Airtime not yet used". They always add up to the total.
- **Others:** anything bought for, or sent to, a number that is not in *My numbers* (Settings). Grouped by number; tap Name to label a number (for example "Mom").
- **My numbers** are learned from messages ("your service number …", "your telebirr Account …") and can be edited. Add a second SIM by hand if it is not detected.
- **One purchase = one expense:** messages are merged by transaction number first, then by amount, category and time.
- Airtime sent between your own numbers is shown in All but is neither an expense nor Others.
