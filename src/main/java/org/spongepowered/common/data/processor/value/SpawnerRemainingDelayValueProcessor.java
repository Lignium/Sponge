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
package org.spongepowered.common.data.processor.value;

import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.BoundedValue.Mutable;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.common.data.processor.common.AbstractSpongeValueProcessor;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.mixin.core.tileentity.MobSpawnerBaseLogicAccessor;
import org.spongepowered.common.mixin.core.tileentity.TileEntityMobSpawnerAccessor;
import org.spongepowered.common.util.Constants;

import java.util.Optional;

public class SpawnerRemainingDelayValueProcessor extends AbstractSpongeValueProcessor<TileEntityMobSpawnerAccessor, Short, Mutable<Short>> {

    public SpawnerRemainingDelayValueProcessor() {
        super(TileEntityMobSpawnerAccessor.class, Keys.SPAWNER_REMAINING_DELAY);
    }

    @Override
    protected Mutable<Short> constructValue(final Short actualValue) {
        return SpongeValueFactory.boundedBuilder(this.key)
                .minimum((short) 0)
                .maximum(Short.MAX_VALUE)
                .defaultValue(Constants.TileEntity.Spawner.DEFAULT_REMAINING_DELAY)
                .actualValue(actualValue)
                .build();
    }

    @Override
    protected boolean set(final TileEntityMobSpawnerAccessor container, final Short value) {
        ((MobSpawnerBaseLogicAccessor) container.accessor$getSpawnerLogic()).accessor$setSpawnDelay(value);
        return true;
    }

    @Override
    protected Optional<Short> getVal(final TileEntityMobSpawnerAccessor container) {
        return Optional.of((short) ((MobSpawnerBaseLogicAccessor) container.accessor$getSpawnerLogic()).accessor$getSpawnDelay());
    }

    @Override
    protected Immutable<Short> constructImmutableValue(final Short value) {
        return this.constructValue(value).asImmutable();
    }

    @Override
    public DataTransactionResult removeFrom(final ValueContainer<?> container) {
        return DataTransactionResult.failNoData();
    }
}
