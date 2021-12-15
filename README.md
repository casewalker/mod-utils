# Mod Utils

This repository will hold custom utilities useful in Minecraft modding. Currently, it contains:

* A watch service to detect changes to a given file
* A configuration handler which can hot-reload from a configuration file

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

## License

Licensed under the MIT License (MIT). Copyright © 2021 Case Walker.