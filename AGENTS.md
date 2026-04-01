# Xeres Development Guidelines

## Project Overview

Xeres is a Friend-to-Friend, decentralized, and secure communication application. It's a Gradle-based Java project with three subprojects:

- **app**: Main Spring Boot application with business logic
- **ui**: JavaFX desktop UI
- **common**: Shared code used by both app and ui

## Build Commands

```bash
# Run the application
./gradlew bootRun

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run tests with UI (if applicable)
./gradlew :ui:test

# Package the application (creates MSI on Windows, AppImage on Linux)
./gradlew :app:jpackage

# Create portable zip
./gradlew :app:jpackage -Pjpackage.portable=true

# Clean build
./gradlew clean

# Build Docker image
./gradlew :app:bootBuildImage
```

## Architecture

- Java 25
- Spring Boot 4.0.5
- JavaFX 26 (UI module)
- JUnit 6 for testing
- ArchUnit for architecture testing
- Jacoco for code coverage
- H2 database with Flyway migrations
- BouncyCastle for cryptography

## Code Conventions

- Follow existing code style (enforced by .editorconfig, Allman Style)
- Use GPL v3 license header on new files
- Branch naming: `feature/<issue-number>-description` or `bugfix/<issue-number>-description`
- Package structure: `io.xeres.<module>.<feature>`

## Key Directories

```
app/src/main/java/io/xeres/app/       - Application entry point and services
ui/src/main/java/io/xeres/ui/         - JavaFX controllers and views
common/src/main/java/io/xeres/common/  - Shared models and utilities
app/src/main/resources/db/migration/   - Flyway database migrations
```

## Testing

- Unit tests use JUnit 6 with Jupiter
- UI tests use TestFX
- Architecture rules are enforced via ArchUnit in `common/src/test/` and `common/src/testFixtures/`

## Dependencies

- Never modify versions directly; update in `build.gradle` root version properties
- Keep Spring Boot BOM and related dependencies in sync
