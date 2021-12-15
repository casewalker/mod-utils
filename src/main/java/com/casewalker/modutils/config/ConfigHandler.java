/*
 * Licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Case Walker.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.casewalker.modutils.config;

import com.casewalker.modutils.interfaces.Reloadable;
import com.casewalker.modutils.files.FileWatcher;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A handler class to use in order to access an underlying configuration class (which must be a subclass of
 * {@link AbstractConfig}). The configuration class will be hot-reloadable, so to avoid hanging onto an outdated
 * configuration instance, this handler should always return the latest configuration instance with {@link #get()}.
 *
 * @param <T> The concrete configuration class which extends {@link AbstractConfig}
 * @author Case Walker
 */
public class ConfigHandler<T extends AbstractConfig> implements Reloadable {

    /**
     * Logger for the class.
     */
    private static final Logger LOGGER = LogManager.getLogger(ConfigHandler.class);

    /**
     * GSON instance to reuse between reloads.
     */
    private static final Gson GSON = new Gson();

    /**
     * YAML instance to reuse between reloads.
     */
    private static final Yaml YAML = new Yaml();

    /**
     * Executor service for watching the configuration file.
     */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * File watcher to detect changes to the configuration file.
     */
    private FileWatcher fileWatcher;

    /**
     * Concrete configuration class which this handler provides access to.
     */
    private T config;

    /**
     * The {@link Class<T>} for the concrete configuration class generified by this handler.
     */
    private final Class<T> configClass;

    /**
     * A subscriber to notify when the configuration has been updated.
     */
    private final Set<Reloadable> subscribers;

    /**
     * Create an instance. Provide the {@link Class<T>} object for java/architecture reasons.
     *
     * @param configClass The {@link Class<T>} for the concrete configuration class generified by this handler
     */
    public ConfigHandler(final Class<T> configClass) {
        this.configClass = configClass;
        this.subscribers = new HashSet<>();
    }

    /**
     * Return the configuration, whatever state it is in. If the configuration has not been initialized or if
     * initialization failed, the return value will be null.
     * <p>
     * <strong>Note: do not store the result of this method, the configuration can be updated live from disk; call this
     * getter to ensure the latest configuration changes are respected.</strong>
     *
     * @return An instance of the configuration class (or null)
     */
    public T get() {
        return config;
    }

    /**
     * Initialize the configuration object. Rely on the default configuration path(s).
     */
    public void initialize() {
        final T dummy = getDummyInstance();
        if (dummy != null) {
            initialize(dummy.getDefaultConfigPaths());
        } else {
            LOGGER.error("Could not get dummy instance, initialization is skipped");
        }
    }

    /**
     * Initialize the configuration object using the provided configuration path(s).
     *
     * @param configFiles File location(s) for configuration class configs
     */
    public void initialize(final List<Path> configFiles) {
        if (config != null) {
            return;
        }

        final Path configFile;

        // Grab the first file which exists
        final Optional<Path> existentConfigFile = configFiles.stream()
                .filter(Files::exists)
                .findFirst();

        // If none of the files exist, then grab the first JSON filename (or just the first entry) and create it
        if (existentConfigFile.isPresent()) {
            configFile = existentConfigFile.get();
        } else {
            configFile =  configFiles.stream()
                    .filter(file -> FileSystems.getDefault().getPathMatcher("regex:.*\\.json").matches(file))
                    .findFirst()
                    .orElse(configFiles.get(0));

            createConfigFile(configFile);
        }

        // Load it
        config = loadConfigFromFile(configFile);

        // If the load was successful, begin watching the file
        if (config != null) {
            try {
                fileWatcher = new FileWatcher(configFile, List.of(this));
                EXECUTOR.submit(() -> fileWatcher.beginWatching());
            } catch (IOException e) {
                LOGGER.error("Exception while instantiating the config file watcher", e);
            }
        } else {
            LOGGER.warn("Loading file {} failed, configs will not be hot-reloaded or respected", configFile);
        }
    }

    /**
     * Load an instance of the configuration class from a specified file.
     *
     * @param configFile The file to load the config from
     * @return An instance of the configuration class based on the provided file
     */
    @VisibleForTesting
    T loadConfigFromFile(final Path configFile) {
        try(final InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile))) {

            if (FileSystems.getDefault().getPathMatcher("regex:.*\\.json").matches(configFile)) {
                return GSON.fromJson(reader, configClass);
            } else if (FileSystems.getDefault().getPathMatcher("regex:.*\\.(yml|yaml)").matches(configFile)) {
                return YAML.loadAs(reader, configClass);
            } else {
                LOGGER.error("Config file must be either JSON or YAML, unknown extension on file: {}", configFile);
            }

        } catch (IOException e) {
            LOGGER.error("Could not read from configuration file {}: {}", configFile, e);

        } catch (YAMLException | JsonSyntaxException e) {
            LOGGER.error("Configuration file {} was malformed: {}", configFile, e);
        }

        return null;
    }

    /**
     * Attempts to create a configuration file with an empty configuration at the specified location.
     *
     * @param configFile Path to the location where a configuration file should be created
     */
    private void createConfigFile(final Path configFile) {
        try {
            Files.createFile(configFile);
            Files.writeString(configFile, "{}\n");
            LOGGER.info("Empty config written to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("Could not create or write to config file {} after failing to read: {}", configFile, e);
        }
    }

    /**
     * Attempt to load a new configuration from the configuration file. Notify subscribers if successful.
     */
    @Override
    public void reload() {
        final T newConfig = loadConfigFromFile(fileWatcher.getFile());
        if (newConfig != null && !newConfig.equals(config)) {
            config = newConfig;
            subscribers.forEach(Reloadable::reload);
        }
    }

    /**
     * Set a subscriber to be notified if there is a successful configuration modification.
     *
     * @param subscriber The subscriber to notify when the config gets a valid modification
     */
    public void registerSubscriber(final Reloadable subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Unregister a subscriber.
     *
     * @param subscriber The subscriber to remove from configuration change updates
     * @return True if the subscriber was subscribed to this config handler
     */
    public boolean unregisterSubscriber(final Reloadable subscriber) {
        return subscribers.remove(subscriber);
    }

    /**
     * Get a dummy instance of the concrete configuration class in order to access important instance methods. Reliant
     * on the class having an accessible no-args constructor. Unfortunately this is ugly because it would make more
     * sense for the instance methods to be static, but static methods cannot be overridden, so here we are.
     * <p>
     * todo: If I ever discover a design pattern or something to better deal with this situation, then refactor.
     *
     * @return A dummy instance of the concrete configuration class {@link T}
     */
    @VisibleForTesting
    T getDummyInstance() {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to instantiate a dummy of the target concrete configuration class", e);
            return null;
        }
    }
}
