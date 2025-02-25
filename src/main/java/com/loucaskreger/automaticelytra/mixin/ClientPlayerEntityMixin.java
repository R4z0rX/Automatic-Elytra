package com.loucaskreger.automaticelytra.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Unique
    private static final int CHESTPLATE_INDEX = 6;
    @Unique
    private int lastIndex = -1;

    @Inject(method = "tickMovement", at = @At(
            value = "INVOKE",
            shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;checkGliding()Z"))
    private void onPlayerTickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        // Injects when the elytra should be deployed
        if (!player.isOnGround() &&
                !player.isGliding() &&
                !player.hasStatusEffect(StatusEffects.LEVITATION) &&
                !player.isTouchingWater()) { //&&
                // [Future] Replace with an event that fires before elytra take off.
            this.equipElytra(player, interactionManager);
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "TAIL"))
    private void endTickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (player.isOnGround() || player.isTouchingWater()) {
            player.checkGliding();
            if (this.lastIndex != -1) {
                if (interactionManager != null) {
                    swap(lastIndex, interactionManager, player);
                }
                lastIndex = -1;
            }
        }
    }

    @Unique
    private void equipElytra(ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager) {
        int firstElytraIndex = this.getElytraIndex(player);
        if (firstElytraIndex != -1) {
            this.lastIndex = firstElytraIndex;
            swap(firstElytraIndex, interactionManager, player);
            // Send packet so server knows player is falling
            player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    /**
     * @param player The player
     * @return the first index of an elytra in the specified player's inventory
     */
    @Unique
    private int getElytraIndex(ClientPlayerEntity player) {
        DefaultedList<ItemStack> inv = player.getInventory().main;
        for (int slot : slotArray()) {
            ItemStack stack = inv.get(slot);
            // Avoid broken Elytras
            if (stack.isOf(Items.ELYTRA) && (stack.getDamage() < stack.getMaxDamage() - 1)) {
                return DataSlotToNetworkSlot(slot);
            }
        }
        return -1;
    }

    @Unique
    private static void swap(int slot, ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player) {
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, CHESTPLATE_INDEX, 0, SlotActionType.PICKUP, player);
        interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, player);
    }

    @Unique
    private static int DataSlotToNetworkSlot(int index) {
        if(index == 100)
            index = 8;
        else if(index == 101)
            index = 7;
        else if(index == 102)
            index = 6;
        else if(index == 103)
            index = 5;
        else if(index == -106 || index == 40)
            index = 45;
        else if(index <= 8)
            index += 36;
        else if(index >= 80 && index <= 83)
            index -= 79;
        return index;
    }
    @Unique
    private static int[] slotArray() {
        int[] range = new int[37];
        for (int i = 0; i < 9; i++) range[i] = 8 - i;
        for (int i = 9; i < 36; i++) range[i] = 35 - (i - 9);
        range[36] = 40;
        return range;
    }
}
