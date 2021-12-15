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
import com.google.common.base.Preconditions;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Watch a specific file for changes and updates.
 *
 * @author Case Walker
 */
public class FileWatcher implements Closeable {

    /**
     * Logger for the class.
     */
    private static final Logger LOGGER = LogManager.getLogger(FileWatcher.class);

    /**
     * File to watch.
     */
    private final Path file;

    /**
     * Use a WatchService to monitor changes in the filesystem.
     */
    private final WatchService watchService;

    /**
     * Store subscribers to notify if there are any valid changes.
     */
    private final Set<Reloadable> subscribers;

    /**
     * Store the key associated with watching the watch-file's directory in order to validate watch events.
     */
    private final WatchKey watchKey;

    /**
     * Create an instance with a specific file to watch and a subscriber to notify on changes.
     *
     * @param file        File to be watched
     * @param subscribers Subscriber to notify of changes to the watched file
     * @throws IOException Getting the new watch service or registering the directory could result in an exception
     */
    public FileWatcher(final Path file, final List<Reloadable> subscribers) throws IOException {
        this(file, subscribers, FileSystems.getDefault().newWatchService(), SensitivityWatchEventModifier.MEDIUM);
    }

    /**
     * See {@link FileWatcher#FileWatcher(Path, Reloadable)}. Use this constructor if overriding
     * {@link FileSystems#getDefault()} call to {@link FileSystem#newWatchService()}.
     *
     * @param file         File to be watched
     * @param subscribers  Subscribers to notify of changes to the watched file
     * @param watchService The {@link WatchService} to use for watching the file
     * @param sensitivity  The sensitivity to use inside the watch service
     * @throws IOException Getting the new watch service or registering the directory could result in an exception
     */
    public FileWatcher(
            final Path file,
            final List<Reloadable> subscribers,
            final WatchService watchService,
            final WatchEvent.Modifier sensitivity
    ) throws IOException {
        Preconditions.checkArgument(Files.isRegularFile(file), "File %s must be a regular file", file);
        Preconditions.checkNotNull(subscribers, "Subscribers cannot be null");
        Preconditions.checkArgument(!subscribers.isEmpty(), "Subscribers cannot be empty");

        this.file = file;
        this.subscribers = new HashSet<>();
        this.subscribers.addAll(subscribers);

        final Path directory = file.getParent();
        this.watchService = watchService;
        this.watchKey =
                directory.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_MODIFY}, sensitivity);
    }

    /**
     * Begin an infinite loop watching for changes to the file for this instance.
     */
    @SuppressWarnings("unchecked")
    public void beginWatching() {

        while (true) {
            // Wait for a new WatchKey to be signalled by the WatchService
            final WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                LOGGER.warn("Exception while watching the file", e);
                return;
            }

            if (!key.equals(watchKey)) {
                LOGGER.warn("Received unrecognized WatchKey: {}, expected {}", key, watchKey);
                continue;
            }

            boolean shouldReload = false;
            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent.Kind<?> kind = event.kind();

                // If kind is OVERFLOW, treat it as a possible file modification
                if (kind == OVERFLOW) {
                    shouldReload = true;
                } else {
                    // If the kind is not OVERFLOW, then the type parameter should be Path. Suppress cast warning.
                    final WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    final Path name = pathEvent.context();
                    final Path child = file.getParent().resolve(name);

                    if (child.equals(file)) {
                        shouldReload = true;
                    }
                }
            }

            if (shouldReload) {
                subscribers.forEach(Reloadable::reload);
            }

            // Reset key and break from the infinite loop if the directory is no longer accessible, also close watcher
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    @Override
    public void close() throws IOException {
        watchKey.cancel();
        watchService.close();
    }

    /**
     * Get the file that the service is configured to watch.
     *
     * @return The file
     */
    public Path getFile() {
        return file;
    }


    /**
     * Set a subscriber to be notified if there is a change to the file.
     *
     * @param subscriber A subscriber to notify if there is a change to the file
     */
    public void registerSubscriber(final Reloadable subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Unregister a subscriber.
     *
     * @param subscriber The subscriber to remove from file change notifications
     * @return True if the subscriber was subscribed to this file watcher
     */
    public boolean unregisterSubscriber(final Reloadable subscriber) {
        return subscribers.remove(subscriber);
    }
}
