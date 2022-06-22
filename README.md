# Mod Utils

This repository will hold custom utilities useful in Minecraft modding. Currently, it contains:

* A watch service to detect changes to a given file
* A configuration handler which can hot-reload from a configuration file
* A parent class that can be used to implement a mixin on the `NarratorMode` enum

Note that this mod is built on top of [Fabric](https://fabricmc.net/).

### Configuration Handling

Aren't there enough Minecraft Mod configuration managers already? Well, I made another one.

This configuration handler allows users to:

* Create custom configuration classes with sensible configuration elements
* Populate those configuration classes using JSON or YAML
* Have those classes be hot-reloadable from disk

To use this code for handling configurations in a mod:

#### Create a Concrete Configuration Class
1. Create a java-bean style public class with private instance variables for your configurations
2. Extend `AbstractConfig`
3. Add getters and setters for each configuration
4. Implement the methods `getDefaultConfigPaths` and `equals`


```java
public class MyModConfig extends AbstractConfig {
    
    private int config1;
    private String config2;
    private List<String> config3;
    
    public int getConfig1() {
        return config1;
    }
    public void setConfig1(int x) {
        config1 = x;
    }
    public String getConfig2() {
        return config2;
    }
    public void setConfig2(String x) {
        config2 = x;
    }
    public List<String> getConfig3() {
        return config3;
    }
    public void setConfig3(List<String> x) {
        config3 = x;
    }

    @Override
    public List<Path> getDefaultConfigPaths() {
        return List.of(Path.of("config", "mymod.json"),
                Path.of("config", "mymod.yml"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyModConfig that = (MyModConfig) o;
        return config1 == that.config1 &&
                Objects.equals(config2, that.config2) &&
                Objects.equals(config3, that.config3);
    }
}
```
#### Use the ConfigHandler

1. Create an instance of `ConfigHandler` using your configuration class as the parameter
2. Initialize it (defaults to using the default configuration paths)
3. Optionally, register one or more subscribers to be notified when there is a change detected on the configurations
(subscribers must implement `Reloadable`)
4. Always reference the configurations through `configHandler.get()` in order to get the latest values in case
the configurations get reloaded
```java
public class MyClass implements Reloadable {

    private final ConfigHandler<MyModConfig> configHandler;
    
    public MyClass() {
        configHandler = new ConfigHandler<>(MyModConfig.class);
        configHandler.initialize();
        configHandler.registerSubscriber(this);
        List<String> configStrings = configHandler.get().getConfig3();
        ...
    }
    ...
}
```

### NarratorModeMixin Support

The `NarratorMode` enum defines what narration modes Minecraft offers through the _Accessibility Settings_ options.
Creating a mixin on the enum can allow a mod to define new narration modes.

Given this mod, another mod would need to write the following class definition:
```java
@Mixin(NarratorMode.class)
@Unique
public abstract class MyNarratorModeMixin {

    private static class NarratorModeMixinHelper extends NarratorModeMixinHelperParent {
        @Override
        public NarratorMode narratorModeInvokeInit(
                final String internalName,
                final int internalId,
                final int id,
                final String name) {
            return MyNarratorModeMixin.narratorModeInvokeInit(internalName, internalId, id, name);
        }
    }

    @Shadow
    @Final
    @Mutable
    private static NarratorMode[] field_18183;

    @Shadow
    @Final
    @Mutable
    private static NarratorMode[] VALUES;

    private final static NarratorModeMixinHelper HELPER = new NarratorModeMixinHelper();

    // Custom NarratorModes go here:
    private static final NarratorMode EXAMPLE1 = 
            addNarratorMode("EXAMPLE1", 4, "options.narrator.example1");
    private static final NarratorMode EXAMPLE2 = 
            addNarratorMode("EXAMPLE2", 5, "options.narrator.example2");
    [...]

    @Invoker("<init>")
    public static NarratorMode narratorModeInvokeInit(
            final String internalName,
            final int internalId,
            final int id,
            final String name) {
        throw new AssertionError();
    }

    private static NarratorMode addNarratorMode(final String internalName, final int id, final String name) {
        final Object[] output = HELPER.addNarratorMode(VALUES, field_18183, internalName, id, name);

        VALUES      = (NarratorMode[]) output[0];
        field_18183 = (NarratorMode[]) output[1];
        return        (NarratorMode)   output[2];
    }
}
```
There is still a fair amount of boilerplate required by the implementer, but it can be almost entirely copy-pasted and
the most finicky logic is safely tucked away in the parent class.

Note that the Fabric framework does not allow the new `NarratorMode`(s) to be made easily accessible by being defined
as `public`. Also, and more annoyingly, the `id` does not necessarily match the ordinal or work with the
`NarratorMode.byId(int)` method, unfortunately, so the implementer must be very careful in accessing the new modes,
especially if they may be in an environment with multiple `NarratorMode` mixins.

## Gradle

Use it in your mod! In gradle:
```groovy
	modImplementation "com.casewalker:mod-utils:<VERSION>"
```
Or to include it inside the JAR file:
```groovy
	include(modImplementation("com.casewalker:mod-utils:<VERSION>"))
```

## License

Licensed under the MIT License (MIT). Copyright © 2021 Case Walker.
