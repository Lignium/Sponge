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
package org.spongepowered.common.registry.type.world.gen;

import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.world.gen.feature.BigTreeFeature;
import net.minecraft.world.gen.feature.BirchTreeFeature;
import net.minecraft.world.gen.feature.CanopyTreeFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.MegaJungleFeature;
import net.minecraft.world.gen.feature.MegaPineTree;
import net.minecraft.world.gen.feature.PointyTaigaTreeFeature;
import net.minecraft.world.gen.feature.SavannaTreeFeature;
import net.minecraft.world.gen.feature.ShrubFeature;
import net.minecraft.world.gen.feature.SwampTreeFeature;
import net.minecraft.world.gen.feature.TallTaigaTreeFeature;
import net.minecraft.world.gen.feature.TreeFeature;
import org.spongepowered.api.registry.util.RegisterCatalog;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.world.gen.PopulatorObject;
import org.spongepowered.api.world.gen.type.BiomeTreeType;
import org.spongepowered.api.world.gen.type.BiomeTreeTypes;
import org.spongepowered.common.bridge.world.gen.WorldGenTreesBridge;
import org.spongepowered.common.registry.type.AbstractPrefixAlternateCatalogTypeRegistryModule;
import org.spongepowered.common.world.gen.type.SpongeBiomeTreeType;

import javax.annotation.Nullable;

@RegisterCatalog(BiomeTreeTypes.class)
public class BiomeTreeTypeRegistryModule extends AbstractPrefixAlternateCatalogTypeRegistryModule<BiomeTreeType> {

    public BiomeTreeTypeRegistryModule() {
        super("minecraft");
    }

    @Override
    public void registerDefaults() {
        this.register(this.create("oak", new TreeFeature(false), new BigTreeFeature(false)));
        this.register(this.create("birch", new BirchTreeFeature(false, false), new BirchTreeFeature(false, true)));

        MegaPineTree tall_megapine = new MegaPineTree(false, true);
        MegaPineTree megapine = new MegaPineTree(false, false);

        this.register(this.create("tall_taiga", new TallTaigaTreeFeature(false), tall_megapine));
        this.register(this.create("pointy_taiga", new PointyTaigaTreeFeature(), megapine));

        BlockState jlog = Blocks.LOG.getDefaultState()
            .withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);

        BlockState jleaf = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
            .withProperty(LeavesBlock.CHECK_DECAY, Boolean.valueOf(false));

        BlockState leaf = Blocks.LEAVES.getDefaultState()
            .withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.JUNGLE)
            .withProperty(LeavesBlock.CHECK_DECAY, Boolean.valueOf(false));

        WorldGenTreesBridge trees = (WorldGenTreesBridge) new TreeFeature(false, 4, jlog, jleaf, true);
        trees.bridge$setMinHeight(VariableAmount.baseWithRandomAddition(4, 7));
        MegaJungleFeature mega = new MegaJungleFeature(false, 10, 20, jlog, jleaf);

        this.register(this.create("jungle", (TreeFeature) trees, mega));

        ShrubFeature bush = new ShrubFeature(jlog, leaf);

        this.register(this.create("jungle_bush", bush, null));
        this.register(this.create("savanna", new SavannaTreeFeature(false), null));
        this.register(this.create("canopy", new CanopyTreeFeature(false), null));
        this.register(this.create("swamp", new SwampTreeFeature(), null));
    }

    private SpongeBiomeTreeType create(String name, Feature small, @Nullable Feature large) {
        return new SpongeBiomeTreeType("minecraft:" + name, name, (PopulatorObject) small, (PopulatorObject) large);
    }
}
