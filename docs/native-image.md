# Building as a native image

The spring boot gradle plugin detects when the graal native image plugin is enabled and generates AOT classes.
This only works in --no-gui mode because JavaFX prevents that AOT process from working (spring boot requires a SpringApplication.run() and not our UiStarter stuff) so the AOT process fails.

You can set properties at runtime like that:

	.\app.exe -D"xrs.ui.enabled=false"

Setting them otherwise requires the class to be initialized at build time. I don't know how to do that with the plugin yet. We need JavaFX anyway...

Currently stuck with the appdirs library that uses JNA and it seems it would need some configuration to work properly:

```
Caused by: java.lang.Error: Structure.getFieldOrder() on class com.sun.jna.platform.win32.Guid$GUID returns names ([Data1, Data2, Data3, Data4]) which do not match declared field names ([])
```

Useful: https://tech-stack.com/blog/using-graalvm-in-a-real-world-scenario-techstacks-experience/
Also: https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html

So for now it seems native image will have to wait.