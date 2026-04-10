---
name: java-conventions
description: Code style, naming conventions, license headers, and patterns for Xeres Java project. Covers Allman braces, utility classes, package structure, and field injection rules.
---

# Java Conventions for Xeres

## Code Style

- **Brace Style**: Allman (braces on next line)
- **Indentation**: tabs only
- **Max line length**: 320 characters

## License Header

Every source file must include the GPL v3 header:

```java
/*
 * Copyright (c) [year-range] by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation...
 */
```

## Version

The version of Java used in Java 25.

## Utility Classes

- Must be `final` class
- Private no-arg constructor that throws `UnsupportedOperationException`
- No instance fields

```java
public final class FooUtils
{
	private ProfileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void doSomething(int count)
	{ ...}
}
```

## Naming Conventions

| Type                | Pattern                                        |
|---------------------|------------------------------------------------|
| Services            | `*Service.java`                                |
| Controllers         | `*Controller.java` or `*WindowController.java` |
| REST Controllers    | `*Controller.java` in `api/controller/`        |
| Client classes      | `*Client.java`                                 |
| Utility classes     | `*Utils.java`                                  |
| Fakes/Test fixtures | `*Fakes.java`                                  |
| Mappers             | `*Mapper.java` (static utility class)          |

## Package Structure

`io.xeres.<module>.<feature>`

| Module | Packages                                                                                    |
|--------|---------------------------------------------------------------------------------------------|
| app    | `api`, `application`, `configuration`, `crypto`, `database`, `job`, `net`, `service`, `xrs` |
| ui     | `client`, `controller`, `custom`, `event`, `model`, `support`                               |
| common | `dto`, `events`, `id`, `message`, `rest`, `protocol`                                        |

## Logging

- Use SLF4J (not `java.util.logging`)
- Logger declaration: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- Logging sensitive data is fine with the `debug` facility

## Field Injection

Field injection is prohibited. Use constructor injection instead:

```java
// Bad
@Autowired
private ProfileService profileService;

// Good
private final ProfileService profileService;

public ContactService(ProfileService profileService)
{
	this.profileService = profileService;
}
```
