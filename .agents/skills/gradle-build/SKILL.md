---
name: gradle-build
description: Gradle build configuration for Xeres including build commands, version management, module structure, and key plugins.
---

# Gradle Build for Xeres

## Project Structure

Multi-module Gradle project:

```
Xeres/
├── app/          - Spring Boot application
├── ui/           - JavaFX desktop UI
├── common/       - Shared code
├── build.gradle  - Root configuration
└── settings.gradle
```

## Build Commands

```bash
# Run the application
./gradlew bootRun

# Build without tests
./gradlew build -x test

# Run tests
./gradlew test

# Run UI tests specifically
./gradlew :ui:test

# Package application (MSI on Windows, .deb on Linux)
./gradlew :app:jpackage

# Create portable zip
./gradlew :app:jpackage -Pjpackage.portable=true

# Build Docker image
./gradlew :app:bootBuildImage

# Clean build
./gradlew clean
```

## Version Management

Versions are defined in `gradle/libs.versions.toml`.

Never modify version numbers directly. Update them in `libs.versions.toml`.

## Module Dependencies

```
app    → common
ui     → common
app    ✗→ ui (forbidden by archunit)
```

## Key Plugins

Defined in `gradle/libs.versions.toml`:

- `org.springframework.boot` - Spring Boot
- `org.flywaydb.flyway` - Flyway migrations
- `org.panteleyev.jpackageplugin` - Packaging (MSI/deb/rpm)
- `org.sonarqube` - Code quality
- `com.bakdata.mockito` - Loads Mockito as a Java agent
- `java`, `jacoco` - Applied to all subprojects in the root build.gradle
- `org.openjfx.javafxplugin` - JavaFX (ui module)

## Subproject Configuration

Subprojects inherit a common configuration from root build.gradle. Module-specific settings go in `app/build.gradle`, `ui/build.gradle`, etc.

## Running Application

```bash
# Development mode
./gradlew bootRun
```

JVM args and the `dev` Spring profile are configured in the `bootRun` task of `app/build.gradle`.
