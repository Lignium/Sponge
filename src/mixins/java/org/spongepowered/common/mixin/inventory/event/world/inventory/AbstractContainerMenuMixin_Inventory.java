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
package org.spongepowered.common.mixin.inventory.event.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.bridge.world.entity.player.PlayerBridge;
import org.spongepowered.common.bridge.world.inventory.InventoryMenuBridge;
import org.spongepowered.common.bridge.world.inventory.ViewableInventoryBridge;
import org.spongepowered.common.bridge.world.inventory.container.MenuBridge;
import org.spongepowered.common.bridge.world.inventory.container.TrackedContainerBridge;
import org.spongepowered.common.bridge.world.inventory.container.TrackedInventoryBridge;
import org.spongepowered.common.event.tracking.phase.packet.PacketPhaseUtil;
import org.spongepowered.common.inventory.adapter.InventoryAdapter;
import org.spongepowered.common.inventory.custom.SpongeInventoryMenu;
import org.spongepowered.common.item.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_Inventory implements TrackedContainerBridge, InventoryAdapter, TrackedInventoryBridge {

    // @formatter: off
    @Final @Shadow private NonNullList<ItemStack> lastSlots;
    @Final @Shadow public NonNullList<Slot> slots;
    @Final @Shadow private List<ContainerListener> containerListeners;

    @Shadow public abstract void shadow$sendAllDataToRemote();
    // @formatter: on

    // TrackedContainerBridge

    private boolean impl$shiftCraft = false;
    private ItemStack impl$menuCapture;

    @Override
    public void bridge$setShiftCrafting(final boolean flag) {
        this.impl$shiftCraft = flag;
    }

    @Override
    public boolean bridge$isShiftCrafting() {
        return this.impl$shiftCraft;
    }


    @Nullable private CraftItemEvent.Craft impl$lastCraft = null;

    @Override
    public void bridge$setLastCraft(final CraftItemEvent.Craft event) {
        this.impl$lastCraft = event;
    }

    @Nullable @Override
    public CraftItemEvent.Craft bridge$getLastCraft() {
        return this.impl$lastCraft;
    }

    @Nullable private ItemStack impl$previousCursor = ItemStack.EMPTY;

    @Override
    public void bridge$setPreviousCursor(@Nullable ItemStack stack) {
        this.impl$previousCursor = stack;
    }

    @Override
    public ItemStack bridge$getPreviousCursor() {
        return this.impl$previousCursor;
    }

    private boolean impl$firePreview = true;

    @Override
    public void bridge$setFirePreview(final boolean firePreview) {
        this.impl$firePreview = firePreview;
    }

    @Override
    public boolean bridge$firePreview() {
        return this.impl$firePreview;
    }

    private List<SlotTransaction> impl$capturedCraftPreviewTransactions = new ArrayList<>();

    @Override
    public List<SlotTransaction> bridge$getPreviewTransactions() {
        return this.impl$capturedCraftPreviewTransactions;
    }

    // Detects if a mod overrides detectAndSendChanges
    private boolean impl$captureSuccess = false;

    @Override
    public boolean bridge$capturePossible() {
        return this.impl$captureSuccess;
    }

    @Override
    public void bridge$setCapturePossible() {
        this.impl$captureSuccess = true;
    }

    @Nullable private Object impl$viewed;

    @Override
    public void bridge$trackViewable(Object inventory) {
        if (inventory instanceof Carrier) {
            inventory = ((Carrier) inventory).inventory();
        }
        if (inventory instanceof Inventory) {
            ((Inventory) inventory).asViewable().ifPresent(i -> ((ViewableInventoryBridge) i).viewableBridge$addContainer((AbstractContainerMenu) (Object) this));
        }
        this.impl$setViewed(inventory);
        // TODO else unknown inventory - try to provide wrapper ViewableInventory?
    }

    private void impl$setViewed(@Nullable Object viewed) {
        if (viewed == null) {
            this.impl$unTrackViewable(this.impl$viewed);
        }
        this.impl$viewed = viewed;
    }

    private void impl$unTrackViewable(@Nullable Object inventory) {
        if (inventory instanceof Carrier) {
            inventory = ((Carrier) inventory).inventory();
        }
        if (inventory instanceof Inventory) {
            ((Inventory) inventory).asViewable().ifPresent(i -> ((ViewableInventoryBridge) i).viewableBridge$removeContainer(((AbstractContainerMenu) (Object) this)));
        }
        // TODO else unknown inventory - try to provide wrapper ViewableInventory?
    }

    // Injects/Redirects -------------------------------------------------------------------------

    @Shadow public abstract Slot shadow$getSlot(int slotId);

    // Called when changing a Slot while in creative mode
    // Captures the SlotTransaction for later event
    @Inject(method = "setItem", at = @At(value = "HEAD") )
    private void impl$addTransaction(final int slotId, final int stateId, final ItemStack itemstack, final CallbackInfo ci) {
        if (this.bridge$capturingInventory()) {
            final Slot slot = this.shadow$getSlot(slotId);
            if (slot != null) {
                final ItemStackSnapshot originalItem = ItemStackUtil.snapshotOf(slot.getItem());
                final ItemStackSnapshot newItem = ItemStackUtil.snapshotOf(itemstack);

                final org.spongepowered.api.item.inventory.Slot adapter = this.inventoryAdapter$getSlot(slotId).get();
                this.bridge$getCapturedSlotTransactions().add(new SlotTransaction(adapter, originalItem, newItem));
            }
        }
    }

    @Inject(method = "removed", at = @At(value = "HEAD"))
    private void onOnContainerClosed(Player player, CallbackInfo ci) {
        this.impl$setViewed(null);
    }


    // ClickType.PICKUP or ClickType.QUICK_MOVE with slotId == -999 ; Dropping Items --------------------------------------

    private boolean impl$dropCancelled = false;

    // Called when dropping a full itemstack out of the inventory ; PART 1/3
    // Restores the cursor item if needed
    @Nullable
    @Redirect(method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
                    ordinal = 0))
    private ItemEntity impl$RestoreOnDragDrop(final Player player, final ItemStack itemStackIn, final boolean unused) {
        final ItemStackSnapshot original = ItemStackUtil.snapshotOf(itemStackIn);
        final ItemEntity entityItem = player.drop(itemStackIn, unused);
        if (!((PlayerBridge) player).bridge$shouldRestoreInventory()) {
            return entityItem;
        }
        if (entityItem == null) {
            this.impl$dropCancelled = true;
            PacketPhaseUtil.handleCustomCursor(player, original);
        }
        return entityItem;
    }

    // Called when dropping a full itemstack out of the inventory ; PART 2/3
    // Resets Player and Container for canceled drop
    @Redirect(method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 1))
    private void impl$ClearOnSlot(final AbstractContainerMenu self, final ItemStack itemStackIn, final int doClickParam0, final int doClickParam1, final ClickType doClickParam2, final Player player) {
        if (!this.impl$dropCancelled || !((PlayerBridge) player).bridge$shouldRestoreInventory()) {
            self.setCarried(itemStackIn); // original behaviour
        }
        ((PlayerBridge) player).bridge$shouldRestoreInventory(false);
        this.impl$dropCancelled = false;
    }

    // Called when splitting and dropping an itemstack out of the inventory ; PART 3/3
    // Restores the cursor item if needed and resets Player and Container for canceled drop
    @Redirect(method = "doClick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
                    ordinal = 1))
    @Nullable
    private ItemEntity impl$restoreOnDragSplit(final Player player, final ItemStack itemStackIn, final boolean unused) {
        final ItemEntity entityItem = player.drop(itemStackIn, unused);
        if (!((PlayerBridge) player).bridge$shouldRestoreInventory()) {
            return entityItem;
        }
        if (entityItem == null) {
            ItemStack original;
            if (player.containerMenu.getCarried().isEmpty()) {
                original = itemStackIn;
            } else {
                player.containerMenu.getCarried().grow(1);
                original = player.containerMenu.getCarried();
            }
            player.containerMenu.setCarried(original);
            ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(-1, -1, -1, original));
        }
        ((PlayerBridge) player).bridge$shouldRestoreInventory(false);
        return entityItem;
    }

    // ClickType.THROW ; throwing items out (Q) -------

    private ItemStackSnapshot impl$itemStackSnapshot = ItemStackSnapshot.empty();
    @Nullable private Slot impl$lastSlotUsed = null;

    // Called before the item is thrown ; PART 1/2
    // Captures the original state and affected slot
    @Redirect(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;mayPickup(Lnet/minecraft/world/entity/player/Player;)Z",
            ordinal = 4))
    public boolean onCanTakeStack(final Slot slot, final Player playerIn) {
        boolean readonly = false;
        if (((MenuBridge) this).bridge$isReadonlyMenu(slot)) {
            ((MenuBridge) this).bridge$refreshListeners();
            readonly = true;
        }
        final boolean result = !readonly && slot.mayPickup(playerIn);
        if (result) {
            this.impl$itemStackSnapshot = ItemStackUtil.snapshotOf(slot.getItem());
            this.impl$lastSlotUsed = slot;
        } else {
            this.impl$itemStackSnapshot = ItemStackSnapshot.empty();
            this.impl$lastSlotUsed = null;
        }
        return result;
    }

    // Called dropping the item ; PART 2/2
    // Restores the slot if needed
    @Nullable
    @Redirect(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;",
            ordinal = 3))
    private ItemEntity onThrowClick(final Player player, final ItemStack itemStackIn, final boolean unused) {
        final ItemEntity entityItem = player.drop(itemStackIn, true);
        if (entityItem == null && ((PlayerBridge) player).bridge$shouldRestoreInventory()) {
            final ItemStack original = ItemStackUtil.fromSnapshotToNative(this.impl$itemStackSnapshot);
            this.impl$lastSlotUsed.set(original);
            player.containerMenu.broadcastChanges(); // TODO check if this is needed?
            player.containerMenu.resumeRemoteUpdates();
            ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(player.containerMenu.containerId, player.containerMenu.getStateId(), this.impl$lastSlotUsed.index, original));
        }
        this.impl$itemStackSnapshot = ItemStackSnapshot.empty();
        this.impl$lastSlotUsed = null;
        ((PlayerBridge) player).bridge$shouldRestoreInventory(false);
        return entityItem;
    }

    // ClickType.PICKUP ; pick up item on cursor -------------------------

    // Called when adding items to the cursor (pickup with item on cursor)
    // Captures the previous cursor for later use
    @Inject(method = "doClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V", ordinal = 0))
    private void beforeOnTakeClickWithItem(
            final int slotId, final int dragType, final ClickType clickTypeIn, final Player player, final CallbackInfo ci) {
        this.bridge$setPreviousCursor(player.containerMenu.getCarried().copy()); // capture previous cursor for CraftItemEvent.Craft
    }

    // Called when setting the cursor item (pickup with empty cursor)
    // Captures the previous cursor for later use
    @Inject(method = "doClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V", ordinal = 3))
    private void beforeOnTakeClick(
            final int slotId, final int dragType, final ClickType clickTypeIn, final Player player, final CallbackInfo ci) {
        this.bridge$setPreviousCursor(player.containerMenu.getCarried().copy()); // capture previous cursor for CraftItemEvent.Craft
    }

    // ClickType.THROW (for Crafting) -------------------------
    // Called when taking items out of a slot (only crafting-output slots relevant here)
    // When it is crafting check if it was cancelled and prevent item drop
    @Redirect(method = "doClick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;safeTake(IILnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 0))
    private ItemStack redirectOnTakeThrow(final Slot slot, final int cnt, final int intMaxValue, final Player player) {
        this.bridge$setLastCraft(null);
        final ItemStack stackToDrop = slot.safeTake(cnt, intMaxValue, player);
        CraftItemEvent.Craft lastCraft = this.bridge$getLastCraft();
        if (lastCraft != null) {
            if (slot instanceof ResultSlot) {
                if (lastCraft.isCancelled()) {
                    stackToDrop.setCount(0); // do not drop crafted item when cancelled
                }
            }
        }
        return stackToDrop;
    }

    // ClickType.QUICK_MOVE (for Crafting) -------------------------
    // Called when Shift-Crafting - 2 Injection points
    // Crafting continues until the returned ItemStack is empty OR the returned ItemStack is not the same as the item in the output slot
    @Redirect(method = "doClick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack redirectTransferStackInSlot(final AbstractContainerMenu thisContainer, final Player player, final int slotId) {
        final Slot slot = thisContainer.getSlot(slotId);
        if (!(slot instanceof ResultSlot)) { // is this crafting?
            return thisContainer.quickMoveStack(player, slotId);
        }
        this.bridge$setLastCraft(null);
        this.bridge$setShiftCrafting(true);
        ItemStack result = thisContainer.quickMoveStack(player, slotId);
        CraftItemEvent.Craft lastCraft = this.bridge$getLastCraft();
        if (lastCraft != null) {
            if (lastCraft.isCancelled()) {
                result = ItemStack.EMPTY; // Return empty to stop shift-crafting
            }
        }

        this.bridge$setShiftCrafting(false);

        return result;
    }

    // cleanup after slot click was captured
    @Inject(method = "doClick", at = @At("RETURN"))
    private void impl$onReturn(final int slotId, final int dragType, final ClickType clickTypeIn, final Player player, final CallbackInfo ci) {
        // Reset variables needed for CraftItemEvent.Craft
        this.bridge$setLastCraft(null);
        this.bridge$setPreviousCursor(null);

        // TODO check if when canceling crafting etc. the client is getting informed correctly already - maybe this is not needed
        // previously from CraftingContainerMixin
        if (((Object) this) instanceof CraftingMenu || ((Object) this) instanceof InventoryMenu) {
            for (ContainerListener listener : this.containerListeners) {
                if (slotId == 0) {
                    this.shadow$sendAllDataToRemote();
                } else {
                    listener.slotChanged((AbstractContainerMenu) (Object) this, 0, this.slots.get(0).getItem());
                }
            }

        }
    }

    // Before broadcasting check if a InventoryMenu wants to cancel changes
    @Inject(method = "broadcastChanges", at = @At("HEAD"))
    public void impl$onBroadcastChanges(CallbackInfo ci) {
        this.bridge$setCapturePossible();
        SpongeInventoryMenu menu = ((MenuBridge)this).bridge$getMenu();
        if (menu == null) {
            return; // No menu - no callbacks
        }
        for (int i = 0; i < this.slots.size(); ++i) {
            final Slot slot = this.slots.get(i);
            final ItemStack newStack = slot.getItem();
            ItemStack oldStack = this.lastSlots.get(i);
            if (!ItemStack.matches(oldStack, newStack)) {
                // Check for menu not allowing change in callback
                if (!menu.onChange(newStack, oldStack, ((Container) this), i, slot)) {
                    slot.set(oldStack.copy()); // revert change in slot
                    // and let vanilla handle the rest
                }
            }
        }
    }

    // Before setting the last slot stack to the new stack capture that change for our inventory events
    @Inject(method = "triggerSlotListeners", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;set(ILjava/lang/Object;)Ljava/lang/Object;"))
    public void impl$onBeforeSubmitLastSlot(int var1, ItemStack var2, Supplier<ItemStack> var3, CallbackInfo ci, ItemStack var4) {
        this.impl$capture(var1, var2, var4);
    }

    @Inject(method = "broadcastChanges", at = @At("RETURN"))
    public void impl$afterBroadcastChanges(CallbackInfo ci) {
        // TODO check if this is still needed see ServerScheduler
        if (this instanceof InventoryMenuBridge) {
            ((InventoryMenuBridge) this).bridge$markClean();
        }
    }

    private void impl$capture(Integer index, ItemStack newStack, ItemStack oldStack) {
        if (this.bridge$capturingInventory()) {
            final ItemStackSnapshot originalItem = ItemStackUtil.snapshotOf(oldStack);
            final ItemStackSnapshot newItem = ItemStackUtil.snapshotOf(newStack);

            org.spongepowered.api.item.inventory.Slot adapter;
            try {
                adapter = this.inventoryAdapter$getSlot(index).get();
                SlotTransaction newTransaction = new SlotTransaction(adapter, originalItem, newItem);
                final List<SlotTransaction> previewTransactions = this.bridge$getPreviewTransactions();
                if (this.bridge$isShiftCrafting()) {
                    previewTransactions.add(newTransaction);
                } else {
                    if (!previewTransactions.isEmpty()) { // Check if Preview transaction is this transaction
                        SlotTransaction previewTransaction = previewTransactions.get(0);
                        if (previewTransaction.slot().equals(newTransaction.slot())) {
                            newTransaction = null;
                        }
                    }
                    if (newTransaction != null) {
                        this.bridge$getCapturedSlotTransactions().add(newTransaction);
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                SpongeCommon.logger().error("SlotIndex out of LensBounds! Did the Container change after creation?", e);
            }
        }
    }

}
