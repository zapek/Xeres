---
name: spring-boot-patterns
description: Spring Boot patterns for Xeres including constructor injection, @Transactional boundaries, REST controllers with OpenAPI annotations, and reactive WebClient usage in the UI module.
---

# Spring Boot Patterns for Xeres

## Application Entry Point

```java

@SpringBootApplication(scanBasePackageClasses = {
		io.xeres.app.XeresApplication.class,
		io.xeres.ui.UiStarter.class
})
public class XeresApplication
```

## Service Patterns

### Constructor Injection

Always use constructor injection. Dependencies are `final`.

```java

@Service
public class ContactService
{
	private final ProfileService profileService;
	private final LocationService locationService;

	public ContactService(ProfileService profileService, LocationService locationService)
	{
		this.profileService = profileService;
		this.locationService = locationService;
	}
}
```

### Transactional Boundaries

```java

@Transactional(readOnly = true)
public List<Contact> getContacts()
{ ...}

@Transactional
public void saveContact(Contact contact)
{ ...}
```

### Circular Dependencies

Use `@Lazy` annotation, but avoid them if possible:

```java
public ContactService(@Lazy ProfileService profileService)
{ ...}
```

## REST Controllers

### OpenAPI Annotations

```java

@RestController
@Tag(name = "Profiles", description = "Profile management")
public class ProfileController
{
	@GetMapping("/{id}")
	@Operation(summary = "Get profile by ID")
	@ApiResponse(responseCode = "200", description = "Profile found")
	public ResponseEntity<ProfileDTO> getProfile(@PathVariable long id)
	{ ...}
}
```

### Exception Handling

Use custom exceptions with appropriate HTTP status codes.

## Reactive WebClient Clients (UI Module)

```java

@Component
public class ProfileClient
{
	private WebClient webClient;

	@EventListener
	public void init(StartupEvent event)
	{
		webClient = webClientBuilder.clone()
				.baseUrl(RemoteUtils.getControlUrl() + PROFILES_PATH)
				.build();
	}

	public Mono<Profile> findById(long id)
	{
		return webClient.get()
				.uri("/{id}", id)
				.retrieve()
				.bodyToMono(Profile.class);
	}
}
```

## Configuration Classes

Use `@Configuration` for feature-specific beans. Keep `@Bean` methods small and focused.

## Testing Services

See `junit-testing` skill for testing patterns with mocks.
