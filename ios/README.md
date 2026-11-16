# Kniha jázd v0.6 — iPhone (iOS)

Native SwiftUI app for iPhone 15 and other supported iPhones (iOS 16+).
Source files: Sources/KnihaJazdApp.swift and Sources/PrivateFuel.swift.

## Implemented
- Purple-neon trip cards, scrollable history and full-screen details.
- Active GPS recording with Core Location, including location updates while the app is in the background if permitted. Recording must be started from the foreground.
- Private drives: only the distance is saved. The temporary route and timestamps are discarded when saved or cancelled. Service drives retain start/end city, dates, times, and km.
- Every 224 private km creates a *simulated* outbound and return draft, 112 km each, in the same model day. After the generated arrival time on a weekday, the app offers to save both separately. The unconverted private km remain in the counter. Simulated trips are always identified as simulations in full-screen details.
- Vision OCR from the camera or photo library for fuel receipt amount, liters, date, and fuel type. User reviews before saving.
- Fuel purchase records saved separately; estimated private usage is recalculated from all private km and a configurable l/100 km rate. Matching diesel and gasoline receipts only; purchased but unallocated liters remain visible.
- Vehicle brand, model, year, plate, color, finish, fuel category, and photo can be changed; covered-car garage as fallback.

## Build for a real iPhone 15
**Simulator ZIP cannot be installed directly on a real iPhone.**

1. On a Mac install Xcode and add your Apple Account under Xcode Settings / Accounts.
2. Install XcodeGen with Homebrew: brew install xcodegen
3. Clone this repo; from the ios folder run: xcodegen generate --spec project.yml
4. Open ios/KnihaJazd.xcodeproj, select the KnihaJazd target, and select your own Team under Signing & Capabilities. Change the bundle identifier if necessary.
5. Connect and trust the iPhone 15, select it as the build target, and press Run. Enable iPhone Developer Mode if requested.
6. Allow precise location and start GPS tracking while the app is foregrounded.

GitHub Actions currently produces a **non-installable-on-device iOS Simulator** build only; it also checks the fuel calculation using automated Swift tests.

## Outstanding practical limits
- GPS precision, phone lock, receipt OCR accuracy, and background interruptions need testing on a real device.
- A process crash can lose a currently recording, unsaved trip. Automatic recovery is not implemented.
- The app does not synchronize Android and iOS data.
- Receipt-based fuel usage is an estimated allocation, not a direct consumption sensor.
- There are no scheduled push notifications while the app is closed; model-pair notices are checked when the app opens.
- Modelled trips cannot be presented as real GPS trips; a simulation label is shown in the details.
- Back up your device data before removing or reinstalling the app.
