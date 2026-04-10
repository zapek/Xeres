---
name: dto-mappers
description: DTO and mapper patterns for Xeres using Java records, canonical constructors with validation, and static mapper utility classes.
---

# DTO and Mapper Patterns for Xeres

## DTOs as Records (Java 21+)

Use Java records for immutable DTOs:

```java
public record ProfileDTO(
		long id,
		@NotNull @Size String name,
		String pgpIdentifier,
		Instant created,
		byte[] pgpFingerprint,
		byte[] pgpPublicKeyData,
		boolean accepted,
		Trust trust,
		@JsonInclude(NON_EMPTY) List<LocationDTO> locations
)
{
	public ProfileDTO
	{
		if (locations == null) locations = new ArrayList<>();
	}
}
```

## Canonical Constructor with Validation

```java
public record ProfileDTO(...)
{
	public ProfileDTO
	{
		Objects.requireNonNull(name, "Name must not be null");
		if (locations == null)
		{
			locations = new ArrayList<>();
		}
		locations = List.copyOf(locations);  // Make immutable
	}
}
```

## Mapper Pattern

Static utility class with mapping methods:

```java
public final class ProfileMapper
{
	private ProfileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ProfileDTO toDTO(Profile profile)
	{
		if (profile == null)
		{
			return null;
		}
		return new ProfileDTO(
				profile.getId(),
				profile.getName(),
				// ... other fields
		);
	}

	public static Profile toEntity(ProfileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}
		var profile = new Profile();
		profile.setId(dto.id());
		profile.setName(dto.name());
		// ... other fields
		return profile;
	}
}
```

## Usage

```java
// Entity to DTO
ProfileDTO dto = ProfileMapper.toDTO(profile);

// DTO to Entity
Profile profile = ProfileMapper.toEntity(dto);

// List mapping
List<ProfileDTO> dtos = profiles.stream()
		.map(ProfileMapper::toDTO)
		.toList();
```

## JsonInclude for Optional Fields

```java

@JsonInclude(NON_EMPTY)  // Don't serialize null or empty collections
List<LocationDTO> locations
```

## Validation Annotations

Use Bean Validation on DTO fields:

```java

@NotNull
@Size(min = 1, max = 255)
String name
@Email
String email
@Min(0)
@Max(100)
int percentage
```

## Collection Handling

Always handle null collections in constructor:

```java
public ProfileDTO
{
	if (locations == null)
	{
		locations = new ArrayList<>();
	}
}
```
