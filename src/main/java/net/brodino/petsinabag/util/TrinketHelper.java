package net.brodino.petsinabag.util;

import dev.emi.trinkets.api.TrinketsApi;
import net.brodino.petsinabag.item.ItemManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class TrinketHelper {
    
    public static ItemStack getPetBagFromTrinkets(PlayerEntity player) {
        try {
            var component = TrinketsApi.getTrinketComponent(player);
            if (component.isPresent()) {
                // Use reflection to access the equipped items safely
                var trinketComponent = component.get();
                
                // Check main hand and offhand first
                ItemStack mainHand = player.getMainHandStack();
                if (mainHand.getItem() == ItemManager.PETS_BAG) {
                    return mainHand;
                }
                
                ItemStack offHand = player.getOffHandStack();
                if (offHand.getItem() == ItemManager.PETS_BAG) {
                    return offHand;
                }
                
                // TODO: Properly access trinket slots when API is stable
                // For now, return empty if not in hands
            }
        } catch (Exception e) {
            // Trinkets API access failed, ignore
        }
        return ItemStack.EMPTY;
    }
    
    public static boolean hasPetBagEquipped(PlayerEntity player) {
        return !getPetBagFromTrinkets(player).isEmpty();
    }
}