/*
 * Licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022 Case Walker.
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
package com.casewalker.modutils.util;

import net.minecraft.client.option.NarratorMode;
import net.minecraft.text.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link NarratorModeMixinHelperParent}.
 */
class NarratorModeMixinTest {

    /**
     * Test class extending the {@link NarratorModeMixinHelperParent}.
     */
    private static class NarratorModeMixin extends NarratorModeMixinHelperParent {
        protected NarratorMode narratorModeInvokeInit(String internalName, int internalId, int id, String name) {
            // final class mocking made possible only by the MockMaker extension
            final NarratorMode mock = Mockito.mock(NarratorMode.class);

            when(mock.getId()).thenReturn(id);
            when(mock.getName()).thenReturn(Text.of(name));
            when(mock.name()).thenReturn(internalName);
            when(mock.ordinal()).thenReturn(internalId);

            return mock;
        }
    }

    @Test
    @DisplayName("Test adding a narrator mode successfully")
    void addNewNarratorMode() {
        NarratorMode[] VALUES = new NarratorMode[] {NarratorMode.ALL, NarratorMode.CHAT};
        NarratorMode[] field_18183 = new NarratorMode[] {NarratorMode.ALL, NarratorMode.CHAT};
        NarratorModeMixin mixin = new NarratorModeMixin();
        String internalName = "TEST";
        int id = 5;
        String name = "options.narrator.test";

        Object[] returned = mixin.addNarratorMode(VALUES, field_18183, internalName, id, name);

        NarratorMode[] newVALUES = (NarratorMode[]) returned[0];
        NarratorMode[] new_field_18183 = (NarratorMode[]) returned[1];
        NarratorMode newMode = (NarratorMode) returned[2];
        assertEquals(name, newMode.getName().getString(), "Name (Text) should match");
        assertEquals(id, newMode.getId(), "ID should match");
        assertEquals(internalName, newMode.name(), "Internal name should match");
        assertEquals(field_18183[field_18183.length - 1].ordinal() + 1, newMode.ordinal(),
                "Ordinal should be based on the last entry's ordinal of the field_18183 array");
        assertEquals(newVALUES.length, new_field_18183.length, "Both arrays should have the same size");
        assertEquals(newVALUES.length, VALUES.length + 1, "VALUES array should have one more element");
        assertEquals(newMode, newVALUES[newVALUES.length - 1], "New element in VALUES should be the new mode");
        assertEquals(newMode, new_field_18183[new_field_18183.length - 1],
                "New element in field_18183 should be the new mode");
    }

    @Test
    @DisplayName("Test adding dumb scenario if VALUES and field_18183 are different lengths")
    void addNarratorModeDumbScenarioLengths() {
        NarratorMode[] VALUES = new NarratorMode[] {NarratorMode.OFF};
        NarratorMode[] field_18183 = new NarratorMode[] {NarratorMode.OFF, NarratorMode.SYSTEM};
        NarratorModeMixin mixin = new NarratorModeMixin();
        String internalName = "TEST";
        int id = 5;
        String name = "options.narrator.test";

        Object[] returned = mixin.addNarratorMode(VALUES, field_18183, internalName, id, name);

        NarratorMode[] newVALUES = (NarratorMode[]) returned[0];
        NarratorMode[] new_field_18183 = (NarratorMode[]) returned[1];
        NarratorMode newMode = (NarratorMode) returned[2];
        assertEquals(id, newMode.getId(), "ID should match");
        assertNotEquals(newVALUES.length, new_field_18183.length, "Arrays should be different sizes");
        assertEquals(newVALUES.length, VALUES.length + 1, "VALUES array should have one more element");
        assertEquals(new_field_18183.length, field_18183.length + 1, "field_18183 array should have one more element");
        assertEquals(newMode, newVALUES[newVALUES.length - 1], "New element in VALUES should be the new mode");
        assertEquals(newMode, new_field_18183[new_field_18183.length - 1],
                "New element in field_18183 should be the new mode");
    }

    @Test
    @DisplayName("Test adding a narrator mode if the underlying arrays start out empty")
    void addNarratorModeEmptyArrays() {
        NarratorMode[] VALUES = new NarratorMode[] {};
        NarratorMode[] field_18183 = new NarratorMode[] {};
        NarratorModeMixin mixin = new NarratorModeMixin();
        String internalName = "TEST";
        int id = 5;
        String name = "options.narrator.test";

        Object[] returned = mixin.addNarratorMode(VALUES, field_18183, internalName, id, name);

        NarratorMode[] newVALUES = (NarratorMode[]) returned[0];
        NarratorMode[] new_field_18183 = (NarratorMode[]) returned[1];
        NarratorMode newMode = (NarratorMode) returned[2];
        assertEquals(id, newMode.getId(), "ID should match");
        assertEquals(newVALUES.length, new_field_18183.length, "Arrays should be the same size");
        assertEquals(1, newVALUES.length, "VALUES array should have one element");
        assertEquals(newMode, newVALUES[newVALUES.length - 1], "New element in VALUES should be the new mode");
        assertEquals(newMode, new_field_18183[new_field_18183.length - 1],
                "New element in field_18183 should be the new mode");
    }

    @Test
    @DisplayName("Test adding a narrator mode if the underlying arrays start out null. This should never happen, but " +
            "it may help having the logic present for testing and static initializations.")
    void addNarratorModeNullArrays() {
        NarratorModeMixin mixin = new NarratorModeMixin();
        String internalName = "TEST";
        int id = 5;
        String name = "options.narrator.test";

        Object[] returned = mixin.addNarratorMode(null, null, internalName, id, name);

        NarratorMode[] newVALUES = (NarratorMode[]) returned[0];
        NarratorMode[] new_field_18183 = (NarratorMode[]) returned[1];
        NarratorMode newMode = (NarratorMode) returned[2];
        assertEquals(id, newMode.getId(), "ID should match");
        assertNotNull(newVALUES, "VALUES should not be null");
        assertNotNull(new_field_18183, "field_18183 should not be null");
        assertEquals(newVALUES.length, new_field_18183.length, "Arrays should be the same size");
        assertEquals(1, newVALUES.length, "VALUES array should have one element");
        assertEquals(newMode, newVALUES[newVALUES.length - 1], "New element in VALUES should be the new mode");
        assertEquals(newMode, new_field_18183[new_field_18183.length - 1],
                "New element in field_18183 should be the new mode");
    }
}
