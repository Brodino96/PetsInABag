package net.brodino.petsinabag.item.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class StoredPets {
    private final String entityTypeId;
    private final NbtCompound entityNbt;
    private final String customName;
    private boolean isSummoned;
    
    public StoredPets(String entityTypeId, NbtCompound entityNbt, String customName) {
        this.entityTypeId = entityTypeId;
        this.entityNbt = entityNbt.copy();
        this.customName = customName;
        this.isSummoned = false;
    }
    
    public static StoredPets fromEntity(Entity entity) {
        NbtCompound nbt = new NbtCompound();
        entity.saveNbt(nbt);
        
        String entityTypeId = Registry.ENTITY_TYPE.getId(entity.getType()).toString();
        String customName = entity.hasCustomName() ? entity.getCustomName().getString() : entity.getType().getTranslationKey();
        
        return new StoredPets(entityTypeId, nbt, customName);
    }
    
    public Entity createEntity(World world) {
        EntityType<?> entityType = Registry.ENTITY_TYPE.get(new Identifier(entityTypeId));
        if (entityType == null) {
            return null;
        }
        
        Entity entity = entityType.create(world);
        if (entity != null) {
            entity.readNbt(entityNbt);
        }
        
        return entity;
    }
    
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("entityType", entityTypeId);
        nbt.put("entityData", entityNbt);
        nbt.putString("customName", customName);
        nbt.putBoolean("isSummoned", isSummoned);
        return nbt;
    }
    
    public static StoredPets fromNbt(NbtCompound nbt) {
        String entityTypeId = nbt.getString("entityType");
        NbtCompound entityData = nbt.getCompound("entityData");
        String customName = nbt.getString("customName");
        
        StoredPets data = new StoredPets(entityTypeId, entityData, customName);
        data.isSummoned = nbt.getBoolean("isSummoned");
        return data;
    }
    
    public String getEntityTypeId() {
        return entityTypeId;
    }
    
    public String getCustomName() {
        return customName;
    }
    
    public boolean isSummoned() {
        return isSummoned;
    }
    
    public void setSummoned(boolean summoned) {
        this.isSummoned = summoned;
    }
    
    public NbtCompound getEntityNbt() {
        return entityNbt.copy();
    }
}