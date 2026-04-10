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

Versions are defined in root `build.gradle` ext block:

```groovy
ext {
	set('version.java', 25)
	set('version.spring-boot', '4.0.5')
	// etc.
}
```

Never modify version numbers directly. Update in root build.gradle.

## Module Dependencies

```
app    → common
ui     → common
app    ✗→ ui (forbidden by archunit)
```

## Key Plugins

- `java` - Java compilation
- `application` - Runnable application
- `org.springframework.boot` - Spring Boot
- `io.github.goooler.java` - BOM management
- `jacoco` - Code coverage
- `org.openjfx.javafxplugin` - JavaFX

## Subproject Configuration

Subprojects inherit common configuration from root build.gradle. Module-specific settings go in `app/build.gradle`, `ui/build.gradle`, etc.

## Running Application

```bash
# Development mode with hot reload
./gradlew bootRun

# With specific JVM args
./gradlew bootRun -PjvmArgs="-Xmx512m"
```
