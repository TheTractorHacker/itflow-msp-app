# RivetMSP for iOS

Native iOS companion for the RivetMSP server, a port of the RivetIT iOS app (`RivetIT-Mobile/ios`, v0.9.6 line) with the
RivetMSP-only modules added. It mirrors the Android app (`com.foleyit.itflow`) screen for screen.

- **UI:** SwiftUI, iOS 16+, iPhone and iPad (the phone layout scales up; there is no iPad-specific layout yet).
- **Logic:** `RivetCore`, a Swift package with no UI dependencies (models, API client, report builders, rules). It builds
  and tests on Linux.
- **Project:** generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen) from `project.yml`.
- **Identity:** bundle id `com.foleyit.rivetmsp.ios`, Keychain service of the same name. The MSP Android manifest declares
  no custom URL scheme, so none is registered. Company name and logo come from the connected server (`GET /api/v1/branding`).

## Build

```sh
brew install xcodegen
cd ios
xcodegen generate          # creates RivetMSP.xcodeproj (git-ignored)
open RivetMSP.xcodeproj
swift test                 # or: docker run --rm -v "$PWD":/w -w /w swift:5.10-jammy swift test
```

Set your own development team in Xcode to run on a device. CI (`.github/workflows/ios.yml`) builds for the simulator and
attaches an **unsigned `.ipa`** and a **simulator `.app.zip`** to a GitHub Release for `v*` / `ios-v*` tags.

## What is in it

Everything the RivetIT iOS app has: server setup with certificate confirmation, sign-in with a separate 2FA screen,
dashboard, side menu, tickets (reply with attachments, status, timer, live chat, create with client/contact/category),
clients, assets with barcode scanner, projects, contracts, credentials behind Face ID / Secure Enclave step-up, knowledge
base, alerts, notifications, appointments, search, profile, app lock, Home Screen quick actions.

Added for RivetMSP:

- **Quotes, invoices, expenses** (list, detail, add expense with camera/photo receipt). Hidden when `/me` reports
  `accounting_enabled: false` or the role has no `module_sales` / `module_financial` access.
- **Ticket charges** (list, add with product suggestions) when `ticket_charges_enabled` is not false.
- **Worksheets** (create from template, fill, complete, delete) and **outtake forms** (create, finger-signature, delete),
  plus the client **Files** tab of signed forms.
- **All 16 reports** including the financial, CSAT, RMM health, service desk and utilization reports.
- **Project milestones** toggle.

"Clients" is the user-facing word (as in the Android app); RivetIT's iOS app says "Departments".

## What has and has not been verified

| Part | Status |
|---|---|
| `RivetCore` | Built and unit-tested on Linux and macOS. Quote, invoice, expense and 8 report payloads are copied from a real RivetMSP demo server; the rest follow the Android models and the server handlers. |
| SwiftUI app | **Compiled by CI** for the simulator. **Not exercised**: no screen has been run on a simulator or device by the author. |
| Secure Enclave reveal, camera/scanner, receipt camera, Face ID lock, signature pad | Compiled, **not exercised**. |

## Not in this build

- Push notifications (APNs) and the `pendingDeepLink` push path.
- Passkey sign-in (the button is hidden).
- iPad-specific split layouts.
- Crash-report upload, accent colour choice (Android-only), the home-screen widget.
- Quotes/invoices are read-only, as on Android.
