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
package org.spongepowered.common.mixin.api.mcp.tileentity;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.BlockEntityArchetype;
import org.spongepowered.api.block.entity.BlockEntityType;
import org.spongepowered.api.data.DataManipulator.Mutable;
import org.spongepowered.api.data.persistence.DataContainer;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.persistence.Queries;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.block.SpongeTileEntityArchetypeBuilder;
import org.spongepowered.common.bridge.data.CustomDataHolderBridge;
import org.spongepowered.common.data.persistence.NbtTranslator;
import org.spongepowered.common.data.util.DataUtil;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.SpongeLocatableBlockBuilder;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(net.minecraft.tileentity.TileEntity.class)
public abstract class TileEntityMixin_API implements BlockEntity {

    @Shadow protected boolean tileEntityInvalid;
    @Shadow protected net.minecraft.world.World world;
    @Shadow protected BlockPos pos;

    @Shadow public abstract BlockPos getPos();
    @Shadow public abstract Block getBlockType();
    @Shadow public abstract CompoundNBT writeToNBT(CompoundNBT compound);
    @Shadow public abstract void shadow$markDirty();

    private final BlockEntityType api$TileEntityType = SpongeImplHooks.getTileEntityType(((net.minecraft.tileentity.TileEntity) (Object) this).getClass());
    @Nullable private LocatableBlock api$LocatableBlock;

    @Override
    public Location<World> getLocation() {
        return new Location<>((World) this.world, VecHelper.toVector3i(this.getPos()));
    }

    @Override
    public int getContentVersion() {
        return 1;
    }

    @Override
    public DataContainer toContainer() {
        final DataContainer container = DataContainer.createNew()
            .set(Queries.CONTENT_VERSION, this.getContentVersion())
            .set(Queries.WORLD_ID, ((World) this.world).getUniqueId().toString())
            .set(Queries.POSITION_X, this.getPos().getX())
            .set(Queries.POSITION_Y, this.getPos().getY())
            .set(Queries.POSITION_Z, this.getPos().getZ())
            .set(Constants.TileEntity.TILE_TYPE, this.api$TileEntityType.getId());
        final CompoundNBT compound = new CompoundNBT();
        this.writeToNBT(compound);
        Constants.NBT.filterSpongeCustomData(compound); // We must filter the custom data so it isn't stored twice
        container.set(Constants.Sponge.UNSAFE_NBT, NbtTranslator.getInstance().translateFrom(compound));
        final Collection<Mutable<?, ?>> manipulators = ((CustomDataHolderBridge) this).bridge$getCustomManipulators();
        if (!manipulators.isEmpty()) {
            container.set(Constants.Sponge.DATA_MANIPULATORS, DataUtil.getSerializedManipulatorList(manipulators));
        }
        return container;
    }

    @Override
    public boolean validateRawData(DataView container) {
        return container.contains(Queries.WORLD_ID)
            && container.contains(Queries.POSITION_X)
            && container.contains(Queries.POSITION_Y)
            && container.contains(Queries.POSITION_Z)
            && container.contains(Constants.TileEntity.TILE_TYPE)
            && container.contains(Constants.Sponge.UNSAFE_NBT);
    }

    @Override
    public void setRawData(DataView container) throws InvalidDataException {
        throw new UnsupportedOperationException(); // TODO Data API
    }

    @Override
    public boolean isValid() {
        return !this.tileEntityInvalid;
    }

    @Override
    public void setValid(boolean valid) {
        this.tileEntityInvalid = valid;
    }

    @Override
    public final BlockEntityType getType() {
        return this.api$TileEntityType;
    }

    @Override
    public BlockState getBlock() {
        return (BlockState) this.world.getBlockState(this.getPos());
    }

    public void supplyVanillaManipulators(List<Mutable<?, ?>> manipulators) {

    }

    @Override
    public Collection<Mutable<?, ?>> getContainers() {
        final List<Mutable<?, ?>> list = Lists.newArrayList();
        this.supplyVanillaManipulators(list);
        if (this instanceof CustomDataHolderBridge) {
            list.addAll(((CustomDataHolderBridge) this).bridge$getCustomManipulators());
        }
        return list;
    }

    @Override
    public BlockEntityArchetype createArchetype() {
        return new SpongeTileEntityArchetypeBuilder().tile(this).build();
    }

    @Override
    public LocatableBlock getLocatableBlock() {
        if (this.api$LocatableBlock == null) {
            BlockState blockState = this.getBlock();
            this.api$LocatableBlock = new SpongeLocatableBlockBuilder()
                .world((World) this.world)
                .position(this.pos.getX(), this.pos.getY(), this.pos.getZ())
                .state(blockState)
                .build();
        }

        return this.api$LocatableBlock;
    }

}
