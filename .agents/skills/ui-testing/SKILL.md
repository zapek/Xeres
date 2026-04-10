---
name: ui-testing
description: TestFX patterns for JavaFX controller testing in Xeres including FXML loading with controller factories, mocking reactive clients, and user interaction testing.
---

# UI Testing Patterns for Xeres

## TestFX Setup

UI tests use TestFX with both `ApplicationExtension` and `MockitoExtension`:

```java

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class ContactViewControllerTest
{
	@Mock
	private ProfileClient profileClient;

	@InjectMocks
	private ContactViewController controller;

	@Test
	void testFxmlLoading() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/contact/contact_view.fxml"));
		loader.setControllerFactory(_ -> controller);
		Parent root = loader.load();
		assertThat(root).isNotNull();
	}
}
```

## FXTest Base Class

For tests requiring JavaFX initialization, extend `FXTest`:

```java
class SomeJavaFXTest extends FXTest
{
	@Test
	void test

	javafx components()
	{
		// JavaFX is initialized
	}
}
```

## FXML Loading Pattern

```java

@Test
void initialize_ShouldLoadContacts() throws IOException
{
	// Load FXML with controller factory
	FXMLLoader loader = new FXMLLoader(
			getClass().getResource("/view/contact/contact_view.fxml")
	);
	loader.setControllerFactory(javaClass -> controller);

	// Initialize controller manually for unit-like tests
	controller.initialize();

	// Verify initial state
	assertThat(controller.getContactTreeTableView()).isNotNull();
}
```

## Testing User Interactions

```java

@Test
void clickButton_ShouldTriggerAction()
{
	// Find button in loaded FXML
	var button = lookup("#saveButton").query();

	// Click and verify
	clickOn(button);

	// Verify interaction with mock
	verify(profileClient).save(any(Profile.class));
}
```

## Mocking Reactive Clients

For WebClient-based clients returning `Mono`:

```java
when(profileClient.findById(anyLong()))
		.

thenReturn(Mono.just(testProfile));
```

## See Also

- `junit-testing` skill for basic testing patterns
- `javafx-patterns` skill for controller structure
