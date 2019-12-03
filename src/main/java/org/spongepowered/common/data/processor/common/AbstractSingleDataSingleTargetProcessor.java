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
package org.spongepowered.common.data.processor.common;

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataManipulator.Immutable;
import org.spongepowered.api.data.DataManipulator.Mutable;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.MergeFunction;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.data.value.ValueContainer;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.data.ValueProcessor;
import org.spongepowered.common.data.util.DataUtil;

import java.util.Optional;

public abstract class AbstractSingleDataSingleTargetProcessor<Holder, T, V extends Value<T>, M extends Mutable<M, I>, I extends Immutable<I, M>>
        extends AbstractSingleDataProcessor<T, V, M, I> implements ValueProcessor<T, V> {

    protected final Class<Holder> holderClass;

    protected AbstractSingleDataSingleTargetProcessor(Key<V> key, Class<Holder> holderClass) {
        super(key);
        this.holderClass = checkNotNull(holderClass);
    }

    protected boolean supports(Holder dataHolder) {
        return true;
    }

    protected abstract boolean set(Holder dataHolder, T value);

    protected abstract Optional<T> getVal(Holder dataHolder);

    protected abstract org.spongepowered.api.data.value.Value.Immutable<T> constructImmutableValue(T value);

    @SuppressWarnings("unchecked")
    @Override
    public boolean supports(DataHolder dataHolder) {
        return this.holderClass.isInstance(dataHolder) && this.supports((Holder) dataHolder);
    }

    @Override
    public boolean supports(EntityType entityType) {
        return this.holderClass.isAssignableFrom(entityType.getEntityClass());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public DataTransactionResult set(DataHolder dataHolder, M manipulator, MergeFunction function) {
        if (this.supports(dataHolder)) {
            final DataTransactionResult.Builder builder = DataTransactionResult.builder();
            final Optional<M> old = this.from(dataHolder);
            final M merged = checkNotNull(function).merge(old.orElse(null), manipulator);
            final T newValue = merged.get(this.key).get();
            final V immutableValue = (V) ((org.spongepowered.api.data.value.Value.Mutable) merged.getValue(this.key).get()).asImmutable();
            try {
                if (this.set((Holder) dataHolder, newValue)) {
                    if (old.isPresent()) {
                        builder.replace(old.get().getValues());
                    }
                    return builder.result(DataTransactionResult.Type.SUCCESS).success((org.spongepowered.api.data.value.Value.Immutable<?>) immutableValue).build();
                }
                return builder.result(DataTransactionResult.Type.FAILURE).reject((org.spongepowered.api.data.value.Value.Immutable<?>) immutableValue).build();
            } catch (Exception e) {
                SpongeImpl.getLogger().debug("An exception occurred when setting data: ", e);
                return builder.result(DataTransactionResult.Type.ERROR).reject((org.spongepowered.api.data.value.Value.Immutable<?>) immutableValue).build();
            }
        }
        return DataTransactionResult.failResult(manipulator.getValues());
    }

    @Override
    public Optional<M> fill(DataHolder dataHolder, M manipulator, MergeFunction overlap) {
        if (!this.supports(dataHolder)) {
            return Optional.empty();
        }
        final M merged = checkNotNull(overlap).merge(manipulator.copy(), this.from(dataHolder).orElse(null));
        return Optional.of(manipulator.set(this.key, merged.get(this.key).get()));
    }

    @Override
    public Optional<M> fill(DataContainer container, M m) {
        final T data = DataUtil.getData(container, this.key);
        m.set(this.key, data);
        return Optional.of(m);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<I> with(Key<? extends Value<?>> key, Object value, I immutable) {
        if (immutable.supports(key)) {
            return Optional.of(immutable.asMutable().set(this.key, (T) value).asImmutable());
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<M> from(DataHolder dataHolder) {
        if (!this.supports(dataHolder)) {
            return Optional.empty();
        }
        final Optional<T> optional = this.getVal((Holder) dataHolder);
        if (optional.isPresent()) {
            return Optional.of(this.createManipulator().set(this.key, optional.get()));
        }
        return Optional.empty();
    }

    @Override
    public final Key<V> getKey() {
        return this.key;
    }

    /**
     * Builds a {@link Value} of the type produced by this processor from an
     * input, actual value.
     *
     * @param actualValue The actual value
     * @return The constructed {@link Value}
     */
    protected abstract V constructValue(T actualValue);

    @SuppressWarnings("unchecked")
    @Override
    public final boolean supports(ValueContainer<?> container) {
        return this.holderClass.isInstance(container) && this.supports((Holder) container);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Optional<T> getValueFromContainer(ValueContainer<?> container) {
        if (!this.supports(container)) {
            return Optional.empty();
        }
        return this.getVal((Holder) container);
    }

    @Override
    public Optional<V> getApiValueFromContainer(ValueContainer<?> container) {
        final Optional<T> optionalValue = this.getValueFromContainer(container);
        if(optionalValue.isPresent()) {
            return Optional.of(this.constructValue(optionalValue.get()));
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataTransactionResult offerToStore(ValueContainer<?> container, T value) {
        final org.spongepowered.api.data.value.Value.Immutable<T> newValue = this.constructImmutableValue(value);
        if (this.supports(container)) {
            final DataTransactionResult.Builder builder = DataTransactionResult.builder();
            final Optional<T> oldVal = this.getVal((Holder) container);
            try {
                if (this.set((Holder) container, value)) {
                    if (oldVal.isPresent()) {
                        builder.replace(this.constructImmutableValue(oldVal.get()));
                    }
                    return builder.result(DataTransactionResult.Type.SUCCESS).success(newValue).build();
                }
                return builder.result(DataTransactionResult.Type.FAILURE).reject(newValue).build();
            } catch (Exception e) {
                SpongeImpl.getLogger().debug("An exception occurred when setting data: ", e);
                return builder.result(DataTransactionResult.Type.ERROR).reject(newValue).build();
            }
        }
        return DataTransactionResult.failResult(newValue);
    }

    @Override
    public final DataTransactionResult remove(DataHolder dataHolder) {
        return this.removeFrom(dataHolder);
    }
}
