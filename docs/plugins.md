# Xeres plugins (API 2)

Xeres discovers optional JAR plugins at startup, before Spring creates services or maps REST endpoints. Adding, replacing or removing a JAR requires restarting Xeres. The default directory is `plugins` inside the active Xeres data directory, so it works consistently for installed, portable and development profiles.

## Build and install chess

From the repository root:

```powershell
.\gradlew.bat :app:bootJar :chess-plugin:jar
```

The host application is in `app/build/libs/`; the separate plugin is in `chess-plugin/build/libs/xeres-chess-<version>.jar`. Copy **only the chess plugin JAR** into `<Xeres data directory>/plugins/` (create the folder if necessary), then restart Xeres. Use the plugin built with the same Xeres revision. The host does not bundle any chess code or assets and installers do not automatically install this optional JAR.

To use another folder when starting the application JAR:

```powershell
java -Dxeres.plugins.directory="C:\Xeres\plugins" -jar app/build/libs/<application-jar>.jar
```

For development, stage the plugin and point `bootRun` at that directory:

```powershell
.\gradlew.bat :chess-plugin:stagePlugin
.\gradlew.bat :app:bootRun --args="--xeres.plugins.directory=../build/plugins"
```

`bootRun` runs from the `app` project directory; alternatively use an absolute path. Staging is explicit, so an ordinary build never installs or enables a plugin in your user profile. Keep only one version of each plugin in the folder.

With chess installed and `xrs.service.chess.enabled=true` (the default), the chess tab, invite menus, chess settings and subscriptions are enabled. Without the JAR, the core application runs without these features. Existing chess history and preferences are preserved when uninstalling the plugin.

## Plugin contract

The `plugin-api` module has no Spring or JavaFX dependency. Implement `io.xeres.plugin.XeresPlugin`:

```java
public class ExamplePlugin implements XeresPlugin
{
    public String id() { return "example"; }
    public int apiVersion() { return API_VERSION; }
    public List<Class<?>> components() { return List.of(ExampleConfiguration.class); }
}
```

Provide `META-INF/services/io.xeres.plugin.XeresPlugin`, containing the implementation's fully qualified class name, and the manifest attribute `Xeres-Plugin-Api: 2`. The chess module is a working example. IDs use lowercase letters, digits and hyphens and must begin with a letter.

The loader uses a parent-first class loader: plugins share the host's API and framework classes. Declare host dependencies as `compileOnly`; do not embed Xeres, the API, Spring or JavaFX in a plugin JAR. Returned Spring components/configurations are registered before context refresh and can use constructor injection and Spring lifecycle callbacks. Xeres's network services retain their existing lifecycle and tunnel registration. Services can supply an RsServiceDescriptor for their protocol ID. UI components are returned separately by uiComponents() and registered only in GUI mode. Implement UiPlugin to contribute tabs, settings pages, notification checkboxes, identity actions and message subscriptions. Plugin views use their own resource bundles and class loader.

The host records availability as `xeres.plugins.<id>.enabled` in the Spring environment. These values reflect discovery, not user claims that a missing JAR is installed. The chess adapter additionally respects `xrs.service.chess.enabled`.

Plugins run as trusted code with the same filesystem and network access as Xeres. API versions and duplicate IDs are checked, but this is not a sandbox, signature verifier or guarantee of binary compatibility with internal host classes. A corrupt/incompatible installation stops startup with an error identifying the plugins directory; remove or replace the offending JAR and restart.

## Current scope

The chess JAR contains the engine, protocol service, storage, ratings, REST/STOMP controllers, DTOs, JavaFX views, images, sounds, translations and preferences. The host contains only generic extension interfaces; removing the JAR removes the entire chess feature. Use matching host and plugin builds. For remote UI use, install a matching chess plugin in both the UI instance and remote server; automatic remote capability discovery is not implemented.

## Verification

```powershell
.\gradlew.bat :chess-plugin:test
.\gradlew.bat :app:test --tests "*PluginLoaderTest" --tests "*ApplicationTunnelSharingTest"
.\gradlew.bat :ui:test --tests "*PluginUiServiceTest"
```

Tests cover an absent plugin directory, external provider loading, Spring bean registration, incompatible API descriptors, duplicate IDs and corrupt JARs. The chess integration test loads the actual generated JAR through a separate class loader and checks controller and network-service registration. Existing chess rule, protocol, history, contact, rating and UI tests remain in place.
