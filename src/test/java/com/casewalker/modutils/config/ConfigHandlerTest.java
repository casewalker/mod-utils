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
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link ConfigHandler}.
 *
 * @author Case Walker
 */
class ConfigHandlerTest {

    /**
     * Config handler instance to reuse. Not the best testing practice, but that's ok!
     */
    private static final ConfigHandler<Config> CONFIG_HANDLER = new ConfigHandler<>(Config.class);

    /**
     * In case the reused ConfigHandler object carries an initialized config instance between tests, wipe it.
     */
    @BeforeEach
    void resetConfig() {
        Whitebox.setInternalState(CONFIG_HANDLER, "config", (Object) null);
    }

    @Test
    @DisplayName("Correct creation of config with JSON (loadConfigFromFile)")
    void testJsonCreation() {
        Config config = CONFIG_HANDLER.loadConfigFromFile(Path.of("src", "test", "resources", "config.json"));
        assertNotNull(config, "Config object should not be null");
        assertTrue(config.isModEnabled(), "Mod should be enabled");
        assertTrue(config.isChatEnabled(), "Chat should be enabled");
        assertEquals(5, config.getEnabledPrefixes().size(), "There should be five enabled prefixes");
        assertTrue(config.getDisabledPrefixes().isEmpty(), "Disabled prefixes should be empty");
        assertEquals(1, config.getEnabledRegularExpressions().size(), "Enabled regexes should have one member");
    }

    @Test
    @DisplayName("Correct creation of config with YAML (loadConfigFromFile)")
    void testYamlCreation() {
        Config config = CONFIG_HANDLER.loadConfigFromFile(Path.of("src", "test", "resources", "config.yml"));
        assertNotNull(config, "Config object should not be null");
        assertTrue(config.isModEnabled(), "Mod should be enabled");
        assertTrue(config.isChatEnabled(), "Chat should be enabled");
        assertEquals(5, config.getEnabledPrefixes().size(), "There should be five enabled prefixes");
        assertTrue(config.getDisabledPrefixes().isEmpty(), "Disabled prefixes should be empty");
        assertEquals(2, config.getEnabledRegularExpressions().size(), "Enabled regexes should have one member");
    }

    @Test
    @DisplayName("If the file does not exist, null should be returned (loadConfigFromFile)")
    void testNoFile() throws IOException {
        Path badFile = Path.of("src", "test", "resources", "bad_filename.json");
        assertTrue(Files.notExists(badFile), "Setup: bad file should not exist before calling `loadConfigFromFile`");
        try {
            Config config = CONFIG_HANDLER.loadConfigFromFile(badFile);
            assertNull(config, "Config should be null");
            assertTrue(Files.notExists(badFile), "Bad file should still not exist after calling `loadConfigFromFile`");
        } finally {
            Files.deleteIfExists(badFile);
        }
    }

    @Test
    @DisplayName("If the file contains malformed JSON, null should be returned (loadConfigFromFile)")
    void testBadJson() {
        Config config = CONFIG_HANDLER.loadConfigFromFile(Path.of("src", "test", "resources", "config_bad_json.json"));
        assertNull(config, "Config should be null from malformed JSON");
    }

    @Test
    @DisplayName("If the file contains malformed YAML, null should be returned (loadConfigFromFile)")
    void testBadYaml() {
        Config config = CONFIG_HANDLER.loadConfigFromFile(Path.of("src", "test", "resources", "config_bad_yaml.yml"));
        assertNull(config, "Config should be null from malformed YAML");
    }

    @Test
    @DisplayName("Dummy creation should succeed (getDummyInstance)")
    void testDummyCreation() {
        Config config = CONFIG_HANDLER.getDummyInstance();
        assertNotNull(config, "Dummy config should not be null");
        assertEmptyConfig(config);
    }

    @Test
    @DisplayName("If the config is already set, reloads should be a NOOP " +
            "and trying to initialize again should do nothing (initialize)")
    void testNoopInitialize() throws IOException {
        Path configFile = Path.of("src", "test", "resources", "config_updated.json");
        Files.writeString(configFile, "{\n  \"modEnabled\": true,\n  \"chatEnabled\": false\n}\n");
        CONFIG_HANDLER.initialize(List.of(configFile));
        assertNotNull(CONFIG_HANDLER.get(), "config should not be null using 'config_updated.json'");
        TestSubscriber subscriber = new TestSubscriber();
        CONFIG_HANDLER.registerSubscriber(subscriber);
        CONFIG_HANDLER.reload();
        assertEquals(0, subscriber.reloads, "Subscriber should not see a reload with the file being unchanged");
        CONFIG_HANDLER.initialize(List.of(Path.of("src", "test", "resources", "config.json")));
        CONFIG_HANDLER.reload();
        assertEquals(0, subscriber.reloads,
                "Subscriber should not see a reload from calling initialize with a different file");
        assertEquals(configFile, Whitebox.<FileWatcher>getInternalState(CONFIG_HANDLER, "fileWatcher").getFile(),
                "File should still be the original file after initializing with a different file");
    }

    @Test
    @DisplayName("If the file does not exist, a file will be created and an empty config will be loaded (initialize)")
    void testNoFileOnInitialize() throws IOException {
        Path badFile = Path.of("src", "test", "resources", "bad_filename.json");
        assertTrue(Files.notExists(badFile), "Setup: bad file should not exist before calling `initialize`");
        try {
            CONFIG_HANDLER.initialize(List.of(badFile));
            Config config = CONFIG_HANDLER.get();
            assertNotNull(config, "Config should not be null");
            assertTrue(Files.exists(badFile), "Bad file should exist after calling `initialize`");
            List<String> contents = Files.readAllLines(badFile);
            assertEquals(1, contents.size(), "Bad file should now have one line");
            assertEquals("{}", contents.get(0), "Bad file should contain an empty JSON object");
        } finally {
            Files.deleteIfExists(badFile);
        }
    }

    @Test
    @DisplayName("Reload should update the subscriber if the updated configurations are different (reload)")
    void testSuccessfulReload() throws IOException {
        Path configFile = Path.of("src", "test", "resources", "config_updated.json");
        Files.writeString(configFile, "{\n  \"modEnabled\": true,\n  \"chatEnabled\": true\n}\n");
        CONFIG_HANDLER.initialize(List.of(configFile));
        TestSubscriber subscriber = new TestSubscriber();
        CONFIG_HANDLER.registerSubscriber(subscriber);
        Files.writeString(configFile, "{\n  \"modEnabled\": true,\n  \"chatEnabled\": false\n}\n");
        CONFIG_HANDLER.reload();
        // Account for the possibility that reload gets called twice if the FileWatcher timing is *perfect*
        assertTrue(subscriber.reloads >= 1, "Subscriber should get 1 reload when underlying file is changed");
    }

    @Test
    @DisplayName("Second file is used if first file does not exist for a list of config file paths (initialize)")
    void testInitializeMultipleFiles() {
        Path badFile = Path.of("src", "test", "resources", "bad_filename.json");
        Path goodFile = Path.of("src", "test", "resources", "config.yml");
        assertTrue(Files.notExists(badFile), "Setup: bad file should not exist before calling `initialize`");
        CONFIG_HANDLER.initialize(List.of(badFile, goodFile));
        assertTrue(Files.notExists(badFile), "Bad file should still not exist after calling `initialize` with another existent file");
        assertEquals(goodFile, Whitebox.<FileWatcher>getInternalState(CONFIG_HANDLER, "fileWatcher").getFile(),
                "File should be the second file provided to `initialize`");
    }

    @Test
    @DisplayName("Reload should not update the subscriber if the new config is identical (reload)")
    void testNoopReload() {
        CONFIG_HANDLER.initialize(List.of(Path.of("src", "test", "resources", "config.json")));
        TestSubscriber subscriber = new TestSubscriber();
        CONFIG_HANDLER.registerSubscriber(subscriber);
        CONFIG_HANDLER.reload();
        assertEquals(0, subscriber.reloads, "Subscriber should not get a reload when underlying file is unchanged");
    }

    @Test
    @DisplayName("Config using Boolean object can default a null configuration element to true (loadConfigFromFile)")
    void testConfigBooleanObject() {
        Path tempFile = Path.of("src", "test", "resources", "other_config.json");
        ConfigHandler<OtherConfig> configHandler = new ConfigHandler<>(OtherConfig.class);
        OtherConfig config = configHandler.loadConfigFromFile(tempFile);
        assertNotNull(config, "Config object should not be null");
        assertNull(config.modEnabled, "The configuration element 'modEnabled' should be null");
        assertTrue(config.isModEnabled(), "The value returned by the method 'isModEnabled' should be true");
    }

    private void assertEmptyConfig(final Config config) {
        assertNotNull(config, "Config object should not be null");
        assertFalse(config.isModEnabled(), "mod should be disabled");
        assertFalse(config.isChatEnabled(), "chat should be disabled");
        assertTrue(config.getEnabledPrefixes().isEmpty(), "enabled prefixes should be empty");
        assertTrue(config.getDisabledPrefixes().isEmpty(), "disabled prefixes should be empty");
        assertTrue(config.getEnabledRegularExpressions().isEmpty(), "enabled regexes should be empty");
    }

    /**
     * Private dummy class to count how many times a subscriber has #reload() called.
     */
    private static class TestSubscriber implements Reloadable {
        int reloads = 0;
        public void reload() { reloads++; }
    }

    /**
     * Private dummy concrete configuration class to be used for testing.
     */
    public static class Config extends AbstractConfig {
        private boolean modEnabled;
        private boolean chatEnabled;
        private List<String> enabledPrefixes;
        private List<String> disabledPrefixes;
        private List<String> enabledRegularExpressions;

        public boolean isModEnabled() { return modEnabled; }
        public void setModEnabled(boolean modEnabled) { this.modEnabled = modEnabled; }
        public boolean isChatEnabled() { return chatEnabled; }
        public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
        public List<String> getEnabledPrefixes()
        { return enabledPrefixes == null ? emptyList() : ImmutableList.copyOf(enabledPrefixes); }
        public void setEnabledPrefixes(List<String> enabledPrefixes) { this.enabledPrefixes = enabledPrefixes; }
        public List<String> getDisabledPrefixes()
        { return disabledPrefixes == null ? emptyList() : ImmutableList.copyOf(disabledPrefixes); }
        public void setDisabledPrefixes(List<String> disabledPrefixes) { this.disabledPrefixes = disabledPrefixes; }
        public List<String> getEnabledRegularExpressions()
        { return enabledRegularExpressions == null ? emptyList() : ImmutableList.copyOf(enabledRegularExpressions); }
        public void setEnabledRegularExpressions(List<String> enabledRegularExpressions)
        { this.enabledRegularExpressions = enabledRegularExpressions; }

        @Override
        public List<Path> getDefaultConfigPaths() { return null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Config that = (Config) o;
            return modEnabled == that.modEnabled && chatEnabled == that.chatEnabled
                    && Objects.equals(enabledPrefixes, that.enabledPrefixes) &&
                    Objects.equals(disabledPrefixes, that.disabledPrefixes) &&
                    Objects.equals(enabledRegularExpressions, that.enabledRegularExpressions);
        }
    }

    /**
     * Test a config class with a Boolean object that defaults to "true" if the value is null.
     */
    public static class OtherConfig extends AbstractConfig {
        public Boolean modEnabled;

        public boolean isModEnabled() { return modEnabled == null || modEnabled; }
        public void setModEnabled(Boolean modEnabled) { this.modEnabled = modEnabled; }

        @Override
        public List<Path> getDefaultConfigPaths() { return null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OtherConfig that = (OtherConfig) o;
            return Objects.equals(modEnabled, that.modEnabled);
        }
    }
}
