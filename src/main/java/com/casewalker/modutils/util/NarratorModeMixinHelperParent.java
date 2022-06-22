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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class should be extended inside a mixin which targets the {@link NarratorMode} enum. To add new mode(s), the
 * implementer should create a mixin class which implements this class privately and then adds modes, such as:
 *
 * <pre>{@code
 *   @Mixin(NarratorMode.class)
 *   @Unique
 *   public abstract class MyNarratorModeMixin {
 *
 *       private static class NarratorModeMixinHelper extends NarratorModeMixinHelperParent {
 *           @Override
 *           public NarratorMode narratorModeInvokeInit(
 *                   final String internalName,
 *                   final int internalId,
 *                   final int id,
 *                   final String name) {
 *               return MyNarratorModeMixin.narratorModeInvokeInit(internalName, internalId, id, name);
 *           }
 *       }
 *
 *       @Shadow
 *       @Final
 *       @Mutable
 *       private static NarratorMode[] field_18183;
 *
 *       @Shadow
 *       @Final
 *       @Mutable
 *       private static NarratorMode[] VALUES;
 *
 *       private final static NarratorModeMixinHelper HELPER = new NarratorModeMixinHelper();
 *
 *       // Custom NarratorModes go here:
 *       private static final NarratorMode EXAMPLE1 =
 *               addNarratorMode("EXAMPLE1", 4, "options.narrator.example1");
 *       private static final NarratorMode EXAMPLE2 =
 *               addNarratorMode("EXAMPLE2", 5, "options.narrator.example2");
 *       [...]
 *
 *       @Invoker("<init>")
 *       public static NarratorMode narratorModeInvokeInit(
 *               final String internalName,
 *               final int internalId,
 *               final int id,
 *               final String name) {
 *           throw new AssertionError();
 *       }
 *
 *       private static NarratorMode addNarratorMode(final String internalName, final int id, final String name) {
 *           final Object[] output = HELPER.addNarratorMode(VALUES, field_18183, internalName, id, name);
 *
 *           VALUES      = (NarratorMode[]) output[0];
 *           field_18183 = (NarratorMode[]) output[1];
 *           return        (NarratorMode)   output[2];
 *       }
 *   }
 * }</pre>
 *
 * As well, since the "name" of a narrator mode appears to be a text translation key, language file(s) should be added
 * just as they are added to fabric projects, under:
 * <br/>
 * <code>  * resources/assets/[modid]/lang/[language].json</code>.
 * <p>
 * Note: This class design is inspired by code from <a href="https://github.com/Micalobia">Micalobia</a> and
 * <a href="https://github.com/LudoCrypt">LudoCrypt</a>.
 *
 * @author Case Walker
 */
public abstract class NarratorModeMixinHelperParent {

    /**
     * Invoke the init method of the {@link NarratorMode} enum.
     *
     * @param internalName Internal Java value, AFAIK
     * @param internalId Internal Java value, AFAIK, related to {@link Enum#ordinal()}
     * @param id See {@link #addNarratorMode(NarratorMode[], NarratorMode[], String, int, String)}
     * @param name See {@link #addNarratorMode(NarratorMode[], NarratorMode[], String, int, String)}
     * @return A newly created NarratorMode
     */
    protected abstract NarratorMode narratorModeInvokeInit(final String internalName,
                                                           final int internalId,
                                                           final int id,
                                                           final String name);

    /**
     * This method is intended to be used to add a new value to the {@link NarratorMode} enum.
     *
     * @param VALUES The private member field of the {@link NarratorMode} enum
     * @param field_18183 The Java-internal field used inside the enum
     * @param internalName Used inside the {@link #narratorModeInvokeInit(String, int, int, String)} method
     * @param id The numeric value ID for the new NarratorMode
     * @param name The translatable name of the NarratorMode
     * @return An Object array containing 1) the updated VALUES value, 2) the updated field_18183 value, and 3) the
     * newly created NarratorMode
     */
    public Object[] addNarratorMode(
            final NarratorMode[] VALUES,
            final NarratorMode[] field_18183,
            final String internalName,
            final int id,
            final String name) {
        // In case the internal field and VALUES differ (this should not happen though), copy them separately
        final List<NarratorMode> modesValues =
                VALUES == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(VALUES));
        final List<NarratorMode> modes_field_18183 =
                field_18183 == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(field_18183));

        // Only using field_18183 for ordinal
        final int ordinal =
                modes_field_18183.isEmpty() ? -1 : modes_field_18183.get(modes_field_18183.size() - 1).ordinal();
        final NarratorMode customNarratorMode = narratorModeInvokeInit(internalName, ordinal + 1, id, name);

        modesValues.add(customNarratorMode);
        modes_field_18183.add(customNarratorMode);

        final NarratorMode[] modesValuesArray = modesValues.toArray(new NarratorMode[0]);
        final NarratorMode[] modes_field_18183_Array = modes_field_18183.toArray(new NarratorMode[0]);

        return new Object[] {modesValuesArray, modes_field_18183_Array, customNarratorMode};
    }
}
