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
package org.spongepowered.common.data.processor.value.entity;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.data.value.immutable.ImmutableBoundedValue;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;

import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;

public class ExperienceFromStartOfLevelValueProcessor extends AbstractSpongeValueProcessor<PlayerEntity, Integer, ImmutableBoundedValue<Integer>> {

    public ExperienceFromStartOfLevelValueProcessor() {
        super(PlayerEntity.class, Keys.EXPERIENCE_FROM_START_OF_LEVEL);
    }


    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, Integer value) {
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult removeFrom(ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }

    @Override
    public ImmutableBoundedValue<Integer> constructValue(Integer defaultValue) {
        return SpongeValueFactory.boundedBuilder(Keys.EXPERIENCE_FROM_START_OF_LEVEL)
            .defaultValue(0)
            .actualValue(defaultValue)
            .minimum(0)
            .maximum(Integer.MAX_VALUE)
            .build()
            .asImmutable();
    }

    @Override
    protected boolean set(PlayerEntity container, Integer value) {
        return false;
    }

    @Override
    protected Optional<Integer> getVal(PlayerEntity container) {
        return Optional.of(container.xpBarCap());
    }

    @Override
    protected Immutable<Integer> constructImmutableValue(Integer value) {
        return this.constructValue(value);
    }

}
