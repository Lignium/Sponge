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
package org.spongepowered.common.mixin.api.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.pointer.Pointers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.ticks.LevelTicks;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.fluid.FluidType;
import org.spongepowered.api.scheduler.ScheduledUpdateList;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.SerializationBehavior;
import org.spongepowered.api.world.border.WorldBorder;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.generation.ChunkGenerator;
import org.spongepowered.api.world.server.ChunkManager;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.api.world.storage.ChunkLayout;
import org.spongepowered.api.world.weather.WeatherType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.accessor.world.entity.raid.RaidsAccessor;
import org.spongepowered.common.bridge.server.level.ServerLevelBridge;
import org.spongepowered.common.bridge.world.level.border.WorldBorderBridge;
import org.spongepowered.common.bridge.world.level.storage.PrimaryLevelDataBridge;
import org.spongepowered.common.data.holder.SpongeServerLocationBaseDataHolder;
import org.spongepowered.common.mixin.api.minecraft.world.level.LevelMixin_API;
import org.spongepowered.common.util.VecHelper;
import org.spongepowered.common.world.storage.SpongeChunkLayout;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_API extends LevelMixin_API<org.spongepowered.api.world.server.ServerWorld, ServerLocation> implements
    org.spongepowered.api.world.server.ServerWorld, SpongeServerLocationBaseDataHolder {

    // @formatter:off
    @Shadow @Final private LevelTicks<Block> blockTicks;
    @Shadow @Final private LevelTicks<Fluid> fluidTicks;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow @Final private ServerLevelData serverLevelData;

    @Shadow public abstract void shadow$save(@Nullable ProgressListener p_217445_1_, boolean p_217445_2_, boolean p_217445_3_);
    @Shadow public abstract void shadow$unload(LevelChunk p_217466_1_);
    @Shadow public abstract ServerChunkCache shadow$getChunkSource();
    @Nonnull @Shadow public abstract MinecraftServer shadow$getServer();
    @Nullable @Shadow public abstract Entity shadow$getEntity(UUID p_217461_1_);
    @Shadow public abstract List<net.minecraft.server.level.ServerPlayer> shadow$players();
    @Shadow public abstract Raids shadow$getRaids();
    @Nullable @Shadow public abstract Raid shadow$getRaidAt(BlockPos p_217475_1_);
    @Shadow public abstract long shadow$getSeed();
    // @formatter:on

    private volatile @MonotonicNonNull Pointers api$pointers;

    @Override
    public long seed() {
        return this.shadow$getSeed();
    }

    // World

    @Override
    public boolean isLoaded() {
        return ((ServerLevelBridge) this).bridge$isLoaded();
    }

    // Pointered (via Audience)

    @Override
    public @NotNull Pointers pointers() {
        if (this.api$pointers == null) {
            return this.api$pointers = Pointers.builder()
                .withDynamic(Identity.UUID, this::uniqueId)
                .withDynamic(Identity.NAME, () -> this.key().formatted())
                .withDynamic(Identity.DISPLAY_NAME, () -> this.properties().displayName().orElse(null))
                .build();
        }
        return this.api$pointers;
    }

    // LocationCreator

    @Override
    public ServerLocation location(final Vector3i position) {
        return ServerLocation.of(this, Objects.requireNonNull(position, "position"));
    }

    @Override
    public ServerLocation location(final Vector3d position) {
        return ServerLocation.of(this, Objects.requireNonNull(position, "position"));
    }

    // ServerWorld

    @Override
    public ServerWorldProperties properties() {
        return (ServerWorldProperties) this.shadow$getLevelData();
    }

    @Override
    public ChunkGenerator generator() {
        return (ChunkGenerator) this.shadow$getChunkSource().getGenerator();
    }

    @Override
    public ResourceKey key() {
        return (ResourceKey) (Object) this.shadow$dimension().location();
    }

    @Override
    public Server engine() {
        return (Server) this.shadow$getServer();
    }

    @Override
    public BlockSnapshot createSnapshot(final int x, final int y, final int z) {
        return ((ServerLevelBridge) this).bridge$createSnapshot(x, y, z);
    }

    @Override
    public boolean restoreSnapshot(final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return snapshot.restore(force, Objects.requireNonNull(flag, "flag"));
    }

    @Override
    public boolean restoreSnapshot(final int x, final int y, final int z, final BlockSnapshot snapshot, final boolean force, final BlockChangeFlag flag) {
        return Objects.requireNonNull(snapshot, "snapshot").withLocation(this.location(x, y, z)).restore(force, Objects.requireNonNull(flag, "flag"));
    }

    @Override
    public Path directory() {
        return ((ServerLevelBridge) this).bridge$getLevelSave().getLevelPath(LevelResource.ROOT);
    }

    private boolean save(final boolean flush) {
        final SerializationBehavior behavior = ((PrimaryLevelDataBridge) this.serverLevelData).bridge$serializationBehavior().orElse(SerializationBehavior.AUTOMATIC);
        ((ServerLevelBridge) this).bridge$setManualSave(true);
        this.shadow$save(null, flush, false);
        return !behavior.equals(SerializationBehavior.NONE);
    }

    @Override
    public boolean save() {
        return save(false);
    }

    @Override
    public boolean saveAndFlush() {
        return save(true);
    }

    @Override
    public boolean unloadChunk(final WorldChunk chunk) {
        this.shadow$unload((LevelChunk) Objects.requireNonNull(chunk, "chunk"));
        return true;
    }

    @Override
    public void triggerExplosion(final org.spongepowered.api.world.explosion.Explosion explosion) {
        ((ServerLevelBridge) this).bridge$triggerExplosion(Objects.requireNonNull(explosion, "explosion"));
    }

    @Override
    public Collection<ServerPlayer> players() {
        return Collections.unmodifiableCollection((Collection<ServerPlayer>) (Collection<?>) ImmutableList.copyOf(this.shadow$players()));
    }

    @Override
    public Collection<org.spongepowered.api.entity.Entity> entities() {
        final Iterable<Entity> all = this.entityManager.getEntityGetter().getAll();

        final List<Entity> returningList = StreamSupport.stream(all.spliterator(), false).collect(Collectors.toList());
        return (Collection< org.spongepowered.api.entity.Entity>) (Object) Collections.unmodifiableCollection(returningList);
    }

    @Override
    public Collection<org.spongepowered.api.raid.Raid> raids() {
        return (Collection<org.spongepowered.api.raid.Raid>) (Collection) ((RaidsAccessor) this.shadow$getRaids()).accessor$raidMap().values();
    }

    @Override
    public Optional<org.spongepowered.api.raid.Raid> raidAt(final Vector3i blockPosition) {
        return Optional.ofNullable((org.spongepowered.api.raid.Raid) this.shadow$getRaidAt(VecHelper.toBlockPos(Objects.requireNonNull(blockPosition, "blockPosition"))));
    }

    // Volume

    @Override
    public boolean contains(final int x, final int y, final int z) {
        return ((ServerLevel) (Object) this).isInWorldBounds(new BlockPos(x, y, z));
    }

    // EntityVolume

    @Override
    public Optional<org.spongepowered.api.entity.Entity> entity(final UUID uniqueId) {
        return Optional.ofNullable((org.spongepowered.api.entity.Entity) this.shadow$getEntity(Objects.requireNonNull(uniqueId, "uniqueId")));
    }

    // MutableBlockEntityVolume

    @Override
    public void removeBlockEntity(final int x, final int y, final int z) {
        // *Just* removing the block entity is going to give is a glitchy state that should never happen.
        // So just remove the whole block.
        this.blockEntity(x, y, z).ifPresent(ignored -> this.removeBlock(x, y, z));
    }

    // UpdateableVolume

    @Override
    public ScheduledUpdateList<BlockType> scheduledBlockUpdates() {
        return (ScheduledUpdateList<BlockType>) this.blockTicks;
    }

    @Override
    public ScheduledUpdateList<FluidType> scheduledFluidUpdates() {
        return (ScheduledUpdateList<FluidType>) this.fluidTicks;
    }

    // LocationBaseDataHolder

    @Override
    public ServerLocation impl$dataholder(int x, int y, int z) {
        return ServerLocation.of(this, x, y, z);
    }

    // WeatherUniverse

    @Override
    public void setWeather(final WeatherType type) {
        this.properties().setWeather(Objects.requireNonNull(type, "type"));
    }

    @Override
    public void setWeather(final WeatherType type, final Ticks ticks) {
        this.properties().setWeather(Objects.requireNonNull(type, "type"), Objects.requireNonNull(ticks, "ticks"));
    }

    @Override
    public ChunkLayout chunkLayout() {
        if (this.api$chunkLayout == null) {
            final var height = ((ServerLevel) (Object) this).getHeight();
            final var min = ((ServerLevel) (Object) this).getMinBuildHeight();
            this.api$chunkLayout = new SpongeChunkLayout(min, height);
        }
        return this.api$chunkLayout;
    }

    @Override
    public WorldBorder setBorder(final WorldBorder border) {
        final WorldBorder worldBorder = ((WorldBorderBridge) ((CollisionGetter) this).getWorldBorder()).bridge$applyFrom(border);
        if (worldBorder == null) {
            return (WorldBorder) net.minecraft.world.level.border.WorldBorder.DEFAULT_SETTINGS;
        }
        this.serverLevelData.setWorldBorder((net.minecraft.world.level.border.WorldBorder.Settings) border);
        return worldBorder;
    }

    @Override
    public ChunkManager chunkManager() {
        return (ChunkManager) this.shadow$getChunkSource().chunkMap;
    }

}
