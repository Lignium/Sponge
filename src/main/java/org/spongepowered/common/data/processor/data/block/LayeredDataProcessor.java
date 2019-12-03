/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
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
package org.spongepowered.common.data.processor.data.block;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableLayeredData;
import org.spongepowered.api.data.manipulator.mutable.block.LayeredData;
import org.spongepowered.api.data.value.BoundedValue.Mutable;
import org.spongepowered.common.data.manipulator.mutable.block.SpongeLayeredData;
import org.spongepowered.common.data.processor.common.AbstractBlockOnlyDataProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;

public class LayeredDataProcessor extends AbstractBlockOnlyDataProcessor<Integer, Mutable<Integer>, LayeredData, ImmutableLayeredData> {

    public LayeredDataProcessor() {
        super(Keys.LAYER);
    }

    @Override
    protected Integer getDefaultValue() {
        return 1;
    }

    @Override
    protected LayeredData createManipulator() {
        return new SpongeLayeredData();
    }

    @Override
    protected Mutable<Integer> constructValue(Integer actualValue) {
        return SpongeValueFactory.boundedBuilder(this.key)
                .defaultValue(this.getDefaultValue())
                .minimum(0)
                .maximum(8)
                .actualValue(actualValue)
                .build();
    }

}
