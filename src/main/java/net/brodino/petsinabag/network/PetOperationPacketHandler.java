package net.brodino.petsinabag.network;

import net.brodino.petsinabag.item.data.PetBagData;
import net.brodino.petsinabag.item.data.StoredPets;
import net.brodino.petsinabag.SummonedPetManager;
import net.brodino.petsinabag.item.ItemManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class PetOperationPacketHandler {
    
    public static void handleSummonPet(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int petIndex = buf.readInt();
        
        server.execute(() -> {
            ItemStack bagStack = getBagFromTrinkets(player);
            if (bagStack.isEmpty()) {
                return;
            }
            
            StoredPets petData = PetBagData.getPet(bagStack, petIndex);
            if (petData == null || petData.isSummoned()) {
                return;
            }
            
            Entity entity = petData.createEntity(player.getWorld());
            if (entity != null) {
                // Position entity near player
                entity.setPosition(player.getX(), player.getY(), player.getZ());
                
                if (player.getWorld().spawnEntity(entity)) {
                    // Mark as summoned
                    petData.setSummoned(true);
                    PetBagData.updatePet(bagStack, petIndex, petData);
                    PetBagData.addSummonedPet(bagStack, entity.getUuid());
                    
                    // Register with summoned pet manager
                    SummonedPetManager.addSummonedPet(player.getUuid(), entity.getUuid(), petIndex);
                    
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
                    
                    player.sendMessage(Text.translatable("item.petsinabag.pets_bag.summoned", petData.getCustomName()), true);
                    
                    // Send sync packet to client
                    syncSummonedPetsToClient(player, bagStack);
                }
            }
        });
    }
    
    public static void handleReleasePet(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int petIndex = buf.readInt();
        
        server.execute(() -> {
            ItemStack bagStack = getBagFromTrinkets(player);
            if (bagStack.isEmpty()) {
                return;
            }
            
            StoredPets petData = PetBagData.getPet(bagStack, petIndex);
            if (petData == null) {
                return;
            }
            
            // If pet is summoned, remove it from world first
            if (petData.isSummoned()) {
                SummonedPetManager.recallPet(player.getUuid(), petIndex);
            }
            
            Entity entity = petData.createEntity(player.getWorld());
            if (entity != null) {
                // Position entity near player
                entity.setPosition(player.getX() + 1, player.getY(), player.getZ());
                
                if (player.getWorld().spawnEntity(entity)) {
                    // Remove from bag
                    PetBagData.removePet(bagStack, petIndex);
                    
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.PLAYERS, 0.5f, 0.8f);
                    
                    player.sendMessage(Text.translatable("item.petsinabag.pets_bag.released", petData.getCustomName()), true);
                    
                    // Send sync packet to client
                    syncSummonedPetsToClient(player, bagStack);
                }
            }
        });
    }
    
    public static void handleRecallPet(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        int petIndex = buf.readInt();
        
        server.execute(() -> {
            ItemStack bagStack = getBagFromTrinkets(player);
            if (bagStack.isEmpty()) {
                return;
            }
            
            StoredPets petData = PetBagData.getPet(bagStack, petIndex);
            if (petData == null || !petData.isSummoned()) {
                return;
            }
            
            if (SummonedPetManager.recallPet(player.getUuid(), petIndex)) {
                // Mark as not summoned
                petData.setSummoned(false);
                PetBagData.updatePet(bagStack, petIndex, petData);
                
                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 0.8f);
                
                player.sendMessage(Text.translatable("item.petsinabag.pets_bag.recalled", petData.getCustomName()), true);
                
                // Send sync packet to client
                syncSummonedPetsToClient(player, bagStack);
            }
        });
    }
    
    private static ItemStack getBagFromTrinkets(ServerPlayerEntity player) {
        // Check main hand and offhand first
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() == ItemManager.PETS_BAG) {
            return mainHand;
        }
        
        ItemStack offHand = player.getOffHandStack();
        if (offHand.getItem() == ItemManager.PETS_BAG) {
            return offHand;
        }
        
        // TODO: Check trinket slots when API is properly accessible
        return ItemStack.EMPTY;
    }
    
    private static void syncSummonedPetsToClient(ServerPlayerEntity player, ItemStack bagStack) {
        PacketByteBuf buf = PacketByteBufs.create();
        var summonedUUIDs = PetBagData.getSummonedPetUUIDs(bagStack);
        buf.writeInt(summonedUUIDs.size());
        for (var uuid : summonedUUIDs) {
            buf.writeUuid(uuid);
        }
        
        ServerPlayNetworking.send(player, NetworkHandler.SYNC_SUMMONED_PETS_PACKET, buf);
    }
}