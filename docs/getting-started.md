# Getting Started

## Installation

### Web

Try MorsVitaEst directly in your browser at [ether4o4.github.io/MorsVitaEst](https://ether4o4.github.io/MorsVitaEst).

### Direct Downloads

| Platform | Format | Download |
|----------|--------|----------|
| Android preview | APK | [Latest rolling preview APK](https://github.com/ether4o4/NeverSoft-Services-OS/releases/download/android-preview-latest/MorsVitaEst-android-preview.apk) plus dated previews on [GitHub Releases](https://github.com/ether4o4/NeverSoft-Services-OS/releases) |
| Android release | APK | [GitHub Releases](https://github.com/ether4o4/NeverSoft-Services-OS/releases) |
| iOS | Xcode source build only | No IPA/TestFlight preview is published by CI yet |
| macOS | DMG | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Windows | MSI | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Linux | DEB | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Linux | RPM | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |
| Linux | AppImage | [GitHub Releases](https://github.com/ether4o4/MorsVitaEst/releases) |

### Platform preview notes

The preview APK is Android-only. Each successful preview workflow creates or updates a dated `android-preview-YYYY-MM-DD` GitHub pre-release and also refreshes the rolling `android-preview-latest` direct APK link. The repository does contain an iOS app target, but CI does not currently sign or publish an iOS `.ipa` or TestFlight build, so iOS testing still requires opening `iosApp/iosApp.xcodeproj` in Xcode with an Apple signing team. Some Android/Desktop local-model flows are also intentionally hidden on iOS where the matching engine support is not wired yet.

## First Steps

1. Launch MorsVitaEst — you'll see the chat screen with an animated welcome
2. Start chatting immediately using the **Free** tier (no API key needed)
3. For better models, open **Settings** and add a service (e.g. OpenAI, Gemini, DeepSeek)
4. Enter your API key — MorsVitaEst validates the connection and loads available models automatically

## Adding a Service

1. Open Settings
2. Tap **Add Service** and pick a provider
3. Paste your API key
4. Select a model from the dropdown
5. Drag services to reorder — the first one is your primary, the rest are fallbacks

See [Multi-Service](features/multi-service.md) for the full details on providers and fallback behavior.
