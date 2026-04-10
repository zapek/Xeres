---
name: archunit-rules
description: ArchUnit architecture rules enforced in Xeres including common module rules (logging, utility classes), app module rules (no field injection, RsService naming), and UI module rules (WindowController naming).
---

# ArchUnit Rules for Xeres

Architecture rules are enforced via ArchUnit tests in `common/src/test/` and `common/src/testFixtures/`.

## Running Rules

```bash
./gradlew test --tests "*CodingRulesTest"
```

## Common Module Rules (`CommonCodingRulesTest`)

### Logging

- No `java.util.logging` allowed
- Use SLF4J only

### Logger Declaration

```java
private static final Logger log;  // Correct
Logger logger;  // Wrong
```

### Utility Classes

```java
public final class FooUtils
{  // Correct
	private FooUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
```

### Identifier Classes

Must have `public static final int LENGTH`:

```java
public class ProfileIdentifier extends Identifier
{
	public static final int LENGTH = 32;
}
```

## App Module Rules (`AppCodingRulesTest`)

### No Field Injection

```java
// Allowed
private final ProfileService profileService;

public Service(ProfileService profileService)
{ ...}

// Forbidden
@Autowired
private ProfileService profileService;
```

### RsService Naming

Service subclasses must end with `RsService`:

```java
public class AvatarRsService extends RsService
{
}  // Correct

public class AvatarService extends RsService
{
}   // Wrong
```

### JPA Entities

Must have public or protected no-arg constructor:

```java
@Entity
public class Profile 
{
    protected Profile() { }  // Required
}
```

### Item Classes

- Public no-arg constructor
- `clone()` method implemented
- Meaningful `toString()` method

### No UI Access

`app` module cannot access `ui` module packages:

```java
noClasses().

that().

resideInPackage("io.xeres.app..")
    .

should().

accessClassesThat().

resideInPackage("io.xeres.ui..")
```

## UI Module Rules (`UiCodingRulesTest`)

### WindowController Naming

```java
public class SettingsWindowController
{
}  // Correct

public class SettingsController
{
}          // Wrong
```

### No FileChooser Initial Directory

Use `ChooserUtils` instead:

```java
// Forbidden
fileChooser.setInitialDirectory(path);

// Use instead
ChooserUtils.

setInitialDirectory(fileChooser, path);
```

### No Field Injection

Same rule as app module.
