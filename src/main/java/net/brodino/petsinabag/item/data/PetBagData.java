package net.brodino.petsinabag.item.data;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PetBagData {
    private static final String PETS_NBT_KEY = "StoredPets";
    private static final String SUMMONED_PETS_NBT_KEY = "SummonedPets";

    public static List<StoredPets> getStoredPets(ItemStack stack) {
        List<StoredPets> pets = new ArrayList<>();

        if (!stack.hasNbt()) {
            return pets;
        }

        NbtCompound nbt = stack.getNbt();
        if (!nbt.contains(PETS_NBT_KEY)) {
            return pets;
        }

        NbtList petsList = nbt.getList(PETS_NBT_KEY, 10); // 10 = NBT_COMPOUND
        for (int i = 0; i < petsList.size(); i++) {
            NbtCompound petNbt = petsList.getCompound(i);
            pets.add(StoredPets.fromNbt(petNbt));
        }

        return pets;
    }

    public static boolean addPet(ItemStack stack, Entity entity) {
        List<StoredPets> pets = getStoredPets(stack);

        // Check if we've reached the maximum capacity (configurable later)
        if (pets.size() >= getMaxCapacity()) {
            return false;
        }

        StoredPets petData = StoredPets.fromEntity(entity);
        pets.add(petData);
        PetBagData.setStoredPets(stack, pets);

        return true;
    }

    public static void setStoredPets(ItemStack stack, List<StoredPets> pets) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList petsList = new NbtList();

        for (StoredPets pet : pets) {
            petsList.add(pet.toNbt());
        }

        nbt.put(PETS_NBT_KEY, petsList);
    }


    public static boolean removePet(ItemStack stack, int index) {
        List<StoredPets> pets = getStoredPets(stack);
        
        if (index < 0 || index >= pets.size()) {
            return false;
        }
        
        pets.remove(index);
        setStoredPets(stack, pets);
        
        return true;
    }
    
    public static StoredPets getPet(ItemStack stack, int index) {
        List<StoredPets> pets = getStoredPets(stack);
        
        if (index < 0 || index >= pets.size()) {
            return null;
        }
        
        return pets.get(index);
    }
    
    public static void updatePet(ItemStack stack, int index, StoredPets petData) {
        List<StoredPets> pets = getStoredPets(stack);
        
        if (index >= 0 && index < pets.size()) {
            pets.set(index, petData);
            setStoredPets(stack, pets);
        }
    }
    
    public static List<UUID> getSummonedPetUUIDs(ItemStack stack) {
        List<UUID> uuids = new ArrayList<>();
        
        if (!stack.hasNbt()) {
            return uuids;
        }
        
        NbtCompound nbt = stack.getNbt();
        if (!nbt.contains(SUMMONED_PETS_NBT_KEY)) {
            return uuids;
        }
        
        NbtList uuidsList = nbt.getList(SUMMONED_PETS_NBT_KEY, 8); // 8 = NBT_STRING
        for (int i = 0; i < uuidsList.size(); i++) {
            try {
                UUID uuid = UUID.fromString(uuidsList.getString(i));
                uuids.add(uuid);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        }
        
        return uuids;
    }
    
    public static void setSummonedPetUUIDs(ItemStack stack, List<UUID> uuids) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList uuidsList = new NbtList();
        
        for (UUID uuid : uuids) {
            uuidsList.add(NbtString.of(uuid.toString()));
        }
        
        nbt.put(SUMMONED_PETS_NBT_KEY, uuidsList);
    }
    
    public static void addSummonedPet(ItemStack stack, UUID petUUID) {
        List<UUID> uuids = getSummonedPetUUIDs(stack);
        if (!uuids.contains(petUUID)) {
            uuids.add(petUUID);
            setSummonedPetUUIDs(stack, uuids);
        }
    }
    
    public static void removeSummonedPet(ItemStack stack, UUID petUUID) {
        List<UUID> uuids = getSummonedPetUUIDs(stack);
        uuids.remove(petUUID);
        setSummonedPetUUIDs(stack, uuids);
    }

    public static int getMaxCapacity() {
        return 9; // Single column like hotbar
    }

    public static int getPetCount(ItemStack stack) {
        return getStoredPets(stack).size();
    }
}