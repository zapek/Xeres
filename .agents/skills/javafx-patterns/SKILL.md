---
name: javafx-patterns
description: JavaFX patterns for Xeres including controller structure with FXML views, WindowController lifecycle, WindowManager usage, and JavaFX-Spring integration.
---

# JavaFX Patterns for Xeres

## Controller Structure

Controllers are Spring components with FXML views:

```java

@Component
@FxmlView(value = "/view/contact/contact_view.fxml")
public class ContactViewController implements Controller
{
	@FXML
	private TreeTableView<Contact> contactTreeTableView;

	@Override
	public void initialize()
	{ ...}
}
```

## Window Controllers

For windows/dialogs, implement `WindowController` interface:

```java

@Component
@FxmlView(value = "/view/settings/settings_window.fxml")
public class SettingsWindowController implements WindowController
{

	@Override
	public void onShowing()
	{ ...}

	@Override
	public void onShown()
	{ ...}

	@Override
	public void onClose()
	{ ...}
}
```

Naming convention: `*WindowController`

## Window Management

Use `WindowManager` for window lifecycle.

## JavaFX-App Integration

```java
public class JavaFxApplication extends Application
{
	private ConfigurableApplicationContext springContext;

	@Override
	public void init()
	{
		springContext = new SpringApplicationBuilder()
				.sources(springApplicationClass)
				.headless(false)
				.initializers(initializers())
				.run(getParameters().getRaw().toArray(new String[0]));
	}
}
```

## File Choosers

Never call `FileChooser.setInitialDirectory()` directly. Use `ChooserUtils`:

```java
// Bad
fileChooser.setInitialDirectory(someDirectory);

// Good
ChooserUtils.

setInitialDirectory(fileChooser, someDirectory);
```

## FXML Location Convention

```
ui/src/main/resources/view/<feature>/<feature>_view.fxml
ui/src/main/resources/view/<feature>/<feature>_window.fxml (for dialogs)
```

## Binding Patterns

Use JavaFX properties for observable data:

```java
private final StringProperty nameProperty = new SimpleStringProperty();
private final ObjectProperty<Profile> selectedProfile = new SimpleObjectProperty<>();
```

## UI Event Handling

Dispatch events through Spring's `ApplicationEventPublisher`:

```java
public record ContactSelectedEvent(Contact contact)
{
}
```
