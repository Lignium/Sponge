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
package org.spongepowered.common.world.gen.builders;

import static com.google.common.base.Preconditions.checkNotNull;

import net.minecraft.world.gen.feature.FireFeature;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.gen.populator.NetherFire;


public class NetherFireBuilder implements NetherFire.Builder {

    private VariableAmount count;
    private VariableAmount cluster;

    public NetherFireBuilder() {
        this.reset();
    }

    @Override
    public NetherFire.Builder perChunk(VariableAmount count) {
        this.count = checkNotNull(count, "count");
        return this;
    }

    @Override
    public NetherFire.Builder perCluster(VariableAmount count) {
        this.cluster = checkNotNull(count, "cluster");
        return this;
    }

    @Override
    public NetherFire.Builder from(NetherFire value) {
        return this.perChunk(value.getClustersPerChunk())
            .perCluster(value.getFirePerCluster());
    }

    @Override
    public NetherFire.Builder reset() {
        this.cluster = VariableAmount.fixed(64);
        this.count = VariableAmount.baseWithRandomAddition(1, VariableAmount.baseWithRandomAddition(1, 10));
        return this;
    }

    @Override
    public NetherFire build() throws IllegalStateException {
        NetherFire pop = (NetherFire) new FireFeature();
        pop.setFirePerCluster(this.cluster);
        pop.setClustersPerChunk(this.count);
        return pop;
    }

}
