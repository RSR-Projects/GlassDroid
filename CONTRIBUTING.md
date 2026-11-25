# Contributing to GlassDroid Basic

Thank you for your interest in **GlassDroid Basic** (`com.rsrprojects.glassdroid`), an independent **RSR Projects** fork of **F-Droid Basic**.

## Scope of Contributions

- **GlassDroid-specific UI, Liquid Glass shaders/modifiers, physics animations, customisation, and branding:** Report issues and submit pull requests directly in [RSR-Projects/GlassDroid](https://github.com/RSR-Projects/GlassDroid).
- **Core F-Droid repository, index, or download engine (`libs/`):** Improvements that benefit the wider ecosystem are also encouraged upstream at [fdroid/fdroidclient](https://gitlab.com/fdroid/fdroidclient).

## Before Opening an Issue

1. Search existing open and closed issues to avoid duplicates.
2. Include your **device model**, **Android version**, **GlassDroid Basic version**, and clear reproduction steps (plus screenshots or exported debug logs from **Settings → Export debug log** where helpful).

## Development Setup & Code Style

- **JDK:** 17
- **Gradle:** 9.3.1 (see `gradle/wrapper/gradle-wrapper.properties`; invoke with system `gradle` or run `gradle wrapper` locally if preferred)
- **Kotlin style:** Official Kotlin style (`kotlin.code.style=official` in `gradle.properties`)

Before submitting changes:

```bash
gradle :app:testDebugUnitTest
```

## Licensing & Copyright Preservation

- Changes under `app/` must remain compatible with **GNU GPL v3.0 or later** ([LICENSE](LICENSE)).
- Changes under `libs/` must remain compatible with **Apache License 2.0** ([libs/LICENSE](libs/LICENSE)).
- Preserve all existing upstream F-Droid, Android Open Source Project, Calyx Institute, and contributor copyright headers.
