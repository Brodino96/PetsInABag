package net.brodino.petsinabag;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SummonedPetManager {
    private static final Map<UUID, Map<UUID, Integer>> playerSummonedPets = new ConcurrentHashMap<>();
    
    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            cleanupPlayerPets(handler.getPlayer().getUuid());
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            cleanupAllPets();
        });
    }
    
    public static void addSummonedPet(UUID playerUUID, UUID petUUID, int petIndex) {
        playerSummonedPets.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(petUUID, petIndex);
        
        // Set up pet behavior
        setupPetBehavior(playerUUID, petUUID);
    }
    
    private static void setupPetBehavior(UUID playerUUID, UUID petUUID) {
        if (PetsInABag.SERVER == null) return;
        
        for (ServerWorld world : PetsInABag.SERVER.getWorlds()) {
            Entity entity = world.getEntity(petUUID);
            if (entity instanceof LivingEntity livingEntity) {
                // Add follow behavior for summoned pets
                if (livingEntity instanceof TameableEntity tameable && tameable.isTamed()) {
                    // Tameable entities already have follow behavior
                    return;
                }
                
                // For now, we'll implement basic follow behavior using teleportation
                // TODO: Add proper AI goals using mixins for better follow behavior
                break;
            }
        }
    }
    
    public static boolean recallPet(UUID playerUUID, int petIndex) {
        Map<UUID, Integer> pets = playerSummonedPets.get(playerUUID);
        if (pets == null) return false;
        
        UUID petToRemove = null;
        for (Map.Entry<UUID, Integer> entry : pets.entrySet()) {
            if (entry.getValue() == petIndex) {
                petToRemove = entry.getKey();
                break;
            }
        }
        
        if (petToRemove != null) {
            pets.remove(petToRemove);
            removePetFromWorld(petToRemove);
            return true;
        }
        
        return false;
    }
    
    public static void cleanupPlayerPets(UUID playerUUID) {
        Map<UUID, Integer> pets = playerSummonedPets.remove(playerUUID);
        if (pets != null) {
            for (UUID petUUID : pets.keySet()) {
                removePetFromWorld(petUUID);
            }
            PetsInABag.LOGGER.info("Cleaned up {} summoned pets for disconnected player {}", pets.size(), playerUUID);
        }
    }
    
    private static void cleanupAllPets() {
        int totalPets = 0;
        for (Map<UUID, Integer> pets : playerSummonedPets.values()) {
            totalPets += pets.size();
            for (UUID petUUID : pets.keySet()) {
                removePetFromWorld(petUUID);
            }
        }
        playerSummonedPets.clear();
        PetsInABag.LOGGER.info("Cleaned up {} summoned pets on server shutdown", totalPets);
    }
    
    private static void removePetFromWorld(UUID petUUID) {
        if (PetsInABag.SERVER == null) return;
        
        for (ServerWorld world : PetsInABag.SERVER.getWorlds()) {
            Entity entity = world.getEntity(petUUID);
            if (entity != null) {
                entity.discard();
                break;
            }
        }
    }
    
    public static boolean isPetSummoned(UUID playerUUID, UUID petUUID) {
        Map<UUID, Integer> pets = playerSummonedPets.get(playerUUID);
        return pets != null && pets.containsKey(petUUID);
    }
    
    public static Set<UUID> getSummonedPets(UUID playerUUID) {
        Map<UUID, Integer> pets = playerSummonedPets.get(playerUUID);
        return pets != null ? new HashSet<>(pets.keySet()) : new HashSet<>();
    }
    
    public static boolean isEntitySummonedByAnyPlayer(UUID entityUUID) {
        for (Map<UUID, Integer> pets : playerSummonedPets.values()) {
            if (pets.containsKey(entityUUID)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isEntitySummonedByPlayer(UUID playerUUID, UUID entityUUID) {
        Map<UUID, Integer> pets = playerSummonedPets.get(playerUUID);
        return pets != null && pets.containsKey(entityUUID);
    }
    
    // TODO: Implement proper follow behavior using mixins
    // For now, pets will spawn near the player and stay there
}