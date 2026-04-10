---
name: junit-testing
description: JUnit 6 testing patterns for Xeres including Mockito with constructor injection, test fixtures via *Fakes.java classes, and AssertJ assertions.
---

# JUnit Testing Patterns for Xeres

## Test Structure

- Location: `src/test/java/` mirrors main source structure
- Naming: `*Test.java` suffix
- Framework: JUnit 6 with Jupiter

## Mockito Extension Pattern

```java

@ExtendWith(MockitoExtension.class)
class ContactServiceTest
{
	@Mock
	private ProfileService profileService;

	@InjectMocks
	private ContactService contactService;

	@Test
	void getContacts_ShouldReturnCombinedList()
	{
		when(profileService.getProfiles()).thenReturn(List.of());

		var result = contactService.getContacts();

		assertTrue(result.isEmpty());
	}
}
```

## Key Points

- Use constructor injection (Mockito injects via `@InjectMocks`)
- `@Mock` creates a mock, `@Spy` creates a partial mock
- `when(...).thenReturn(...)` for stubbing
- `verify(...).method()` for interaction testing
- Prefer JUnit assertions: `assertTrue(...)` but it's also possible to use assertJ for more complex cases

## Test Fixtures

Use `*Fakes.java` in `common/src/testFixtures/java/io/xeres/`:

```java
public final class ProfileFakes
{
	private ProfileFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Profile createProfile()
	{
		return createProfile(1L, "Test Profile");
	}

	public static Profile createProfile(long id, String name)
	{
		var profile = new Profile();
		profile.setId(id);
		profile.setName(name);
		return profile;
	}

	public static Profile createOwnProfile()
	{
		var profile = createProfile();
		profile.setOwn(true);
		return profile;
	}
}
```

## Assertion Examples

```java
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

assertNotNull(result);

assertEquals("Test",result.getName);

assertThat(list).

hasSize(2).

contains(profile1, profile2);

assertThatThrownBy(() ->service.

save(null))
		.

isInstanceOf(IllegalArgumentException .class);
```

## Exception Testing

```java

@Test
void save_WithNullProfile_ShouldThrow()
{
	assertThatThrownBy(() -> contactService.save(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("Profile must not be null");
}
```

## See Also

- `ui-testing` skill for JavaFX controller testing
- `archunit-rules` skill for testing architecture rules
