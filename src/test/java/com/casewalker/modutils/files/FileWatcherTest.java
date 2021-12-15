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
package com.casewalker.modutils.files;

import com.casewalker.modutils.interfaces.Reloadable;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test functionality within the {@link FileWatcher}.
 *
 * @author Case Walker
 */
class FileWatcherTest {

    private static final Path TEST_DIR = Path.of("src", "test", "resources", "watcher_test");
    private static final Path TEST_FILE = Path.of("src", "test", "resources", "watcher_test", "file.txt");
    private static final Path OTHER_FILE = Path.of("src", "test", "resources", "watcher_test", "other_file.txt");

    @BeforeAll
    static void setup() throws IOException {
        Files.deleteIfExists(TEST_FILE);
        Files.deleteIfExists(OTHER_FILE);

        if (Files.notExists(TEST_DIR)) {
            Files.createDirectory(TEST_DIR);
        }
        Files.createFile(TEST_FILE);
    }

    @AfterAll
    static void tearDown() throws IOException {
        Files.deleteIfExists(TEST_FILE);
        Files.deleteIfExists(OTHER_FILE);
    }

    @Test
    @DisplayName("Exception if 'file' is a directory (constructor)")
    void constructorWithDir() {
        assertThrows(IllegalArgumentException.class, () -> new FileWatcher(TEST_DIR, List.of(new TestClass())),
                "FileWatcher constructor should throw IllegalArgumentException for non-file `file` arg");
    }

    @Test
    @DisplayName("Exception if 'file' doesn't exist (constructor)")
    void constructorWithNoFile() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileWatcher(Path.of("src", "test", "resources", "watcher_test", "fake.txt"),
                        List.of(new TestClass())),
                "FileWatcher constructor should throw IllegalArgumentException if `file` arg doesn't exist"
        );
    }

    @Test
    @DisplayName("Exception if 'file' is null (constructor)")
    void constructorWithNullFile() {
        assertThrows(NullPointerException.class, () -> new FileWatcher(null, List.of(new TestClass())),
                "FileWatcher constructor should throw NullPointerException if `file` arg is null");
    }

    @Test
    @DisplayName("Exception if subscribers is null (constructor)")
    void constructorWithNullSubscribers() {
        assertThrows(NullPointerException.class, () -> new FileWatcher(TEST_FILE, null),
                "FileWatcher constructor should throw NullPointerException if subscriber is null");
    }

    @Test
    @DisplayName("Exception if subscribers is empty (constructor)")
    void constructorWithEmptySubscribers() {
        assertThrows(IllegalArgumentException.class, () -> new FileWatcher(TEST_FILE, Collections.emptyList()),
                "FileWatcher constructor should throw NullPointerException if subscriber is null");
    }

    @Test
    @DisplayName("All should be well with valid file and subscriber (constructor)")
    void constructorSuccess() throws IOException {
        try (FileWatcher fileWatcher = new FileWatcher(TEST_FILE, List.of(new TestClass()))) {
            assertEquals(TEST_FILE, fileWatcher.getFile(), "File in FileWatcher should match input");
        }
    }

    @Test
    @DisplayName("Counts modifications to original file (beginWatching)")
    void beginWatchingCountModifications() throws IOException, InterruptedException {
        TestClass subscriber = new TestClass();

        try (FileWatcher fileWatcher =
                     new FileWatcher(TEST_FILE, List.of(subscriber), FileSystems.getDefault().newWatchService(),
                             SensitivityWatchEventModifier.HIGH)) {

            new Thread(fileWatcher::beginWatching).start();

            // Add sleeps to allow watch service polling to succeed
            Thread.sleep(2100);
            Files.writeString(TEST_FILE, "testing line 1\n", WRITE, TRUNCATE_EXISTING);
            Thread.sleep(2100);
            Files.writeString(TEST_FILE, "testing append\n", WRITE, APPEND);
            Thread.sleep(2100);
            Files.writeString(TEST_FILE, "testing fresh new line\n", WRITE, TRUNCATE_EXISTING);
            Thread.sleep(2100);
        }
        assertEquals(3, subscriber.reloads, "Subscriber should see 3 reloads");
    }

    @Test
    @DisplayName("Does not count modifications to other files (beginWatching)")
    void beginWatchingDoNotCountNonModifications() throws IOException, InterruptedException {
        TestClass subscriber = new TestClass();

        try (FileWatcher fileWatcher =
                     new FileWatcher(TEST_FILE, List.of(subscriber), FileSystems.getDefault().newWatchService(),
                             SensitivityWatchEventModifier.HIGH)) {

            new Thread(fileWatcher::beginWatching).start();

            // Add sleeps to allow watch service polling to succeed
            Thread.sleep(2100);
            Files.createFile(OTHER_FILE);
            Thread.sleep(2100);
            Files.writeString(OTHER_FILE, "testing line 1\n", WRITE, TRUNCATE_EXISTING);
            Thread.sleep(2100);
        }
        assertEquals(0, subscriber.reloads, "Subscriber should see 0 reloads");
    }

    /**
     * Private dummy class to count how many times a subscriber has #reload() called.
     */
    private static class TestClass implements Reloadable {
        public int reloads = 0;

        public void reload() {
            reloads++;
        }
    }
}
