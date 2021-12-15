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

import java.nio.file.Path;
import java.util.List;

/**
 * Configuration encapsulation.
 * <p>
 * Subclasses should be public classes with private instance variables for each configuration element, along with the
 * appropriate public getters and setters for the element(s). This should allow {@link com.google.gson.Gson} or
 * {@link org.yaml.snakeyaml.Yaml} to correctly deserialize configuration files into concrete objects, as well as
 * allowing other parts of the code to access the configurations.
 * <p>
 * It is required by the {@link ConfigHandler} that subclasses have a public no-arguments constructor. The handler uses
 * a dummy-instance of the subclass to determine the overridden value of {@link #getDefaultConfigPaths()}. This may also
 * get used by the deserializers.
 *
 * @author Case Walker
 */
public abstract class AbstractConfig {

    /**
     * Get the {@link Path}(s) to the default configuration file(s). Because multiple file formats may be supported,
     * this should allow for multiple defaults to be provided.
     *
     * @return The path(s) to the default configuration file(s)
     */
    public abstract List<Path> getDefaultConfigPaths();

    /**
     * Ensure that subclasses override the implementation of {@link Object#equals(Object)} because it is used by the
     * {@link ConfigHandler}.
     */
    @Override
    public abstract boolean equals(Object o);
}
