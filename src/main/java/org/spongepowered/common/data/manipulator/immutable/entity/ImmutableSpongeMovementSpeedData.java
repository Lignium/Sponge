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
package org.spongepowered.common.data.manipulator.immutable.entity;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableMovementSpeedData;
import org.spongepowered.api.data.manipulator.mutable.entity.MovementSpeedData;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.value.Value.Immutable;
import org.spongepowered.common.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeMovementSpeedData;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;

public class ImmutableSpongeMovementSpeedData extends AbstractImmutableData<ImmutableMovementSpeedData, MovementSpeedData> implements ImmutableMovementSpeedData  {

    private final double walkSpeed;
    private final double flySpeed;

    private final Immutable<Double> walkSpeedValue;
    private final Immutable<Double> flyingSpeedValue;


    public ImmutableSpongeMovementSpeedData(double walkSpeed, double flySpeed) {
        super(ImmutableMovementSpeedData.class);
        this.walkSpeed = walkSpeed;
        this.flySpeed = flySpeed;
        this.walkSpeedValue = new ImmutableSpongeValue<>(Keys.WALKING_SPEED, 0.7D, this.walkSpeed);
        this.flyingSpeedValue = new ImmutableSpongeValue<>(Keys.FLYING_SPEED, 0.05D, this.flySpeed);

        this.registerGetters();
    }

    public double getWalkSpeed() {
        return this.walkSpeed;
    }

    public double getFlySpeed() {
        return this.flySpeed;
    }

    @Override
    protected void registerGetters() {
        this.registerFieldGetter(Keys.WALKING_SPEED, ImmutableSpongeMovementSpeedData.this::getWalkSpeed);
        this.registerKeyValue(Keys.WALKING_SPEED, ImmutableSpongeMovementSpeedData.this::walkSpeed);

        this.registerFieldGetter(Keys.FLYING_SPEED, ImmutableSpongeMovementSpeedData.this::getFlySpeed);
        this.registerKeyValue(Keys.FLYING_SPEED, ImmutableSpongeMovementSpeedData.this::flySpeed);
    }

    @Override
    public Immutable<Double> walkSpeed() {
        return this.walkSpeedValue;
    }

    @Override
    public Immutable<Double> flySpeed() {
        return this.flyingSpeedValue;
    }

    @Override
    public MovementSpeedData asMutable() {
        return new SpongeMovementSpeedData(this.walkSpeed, this.flySpeed);
    }

    @Override
    public DataContainer toContainer() {
        return super.toContainer()
            .set(Keys.WALKING_SPEED.getQuery(), this.walkSpeed)
            .set(Keys.FLYING_SPEED.getQuery(), this.flySpeed);
    }
}
