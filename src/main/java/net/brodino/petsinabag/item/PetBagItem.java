package net.brodino.petsinabag.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.brodino.petsinabag.PetsInABag;
import net.brodino.petsinabag.SummonedPetManager;
import net.brodino.petsinabag.item.data.PetBagData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class PetBagItem extends TrinketItem {

    private final int maxCapacity;

    public PetBagItem(Settings settings, int maxCapacity) {
        super(settings);
        this.maxCapacity = maxCapacity;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        World world = user.getWorld();

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        return this.captureEntity(user.getWorld(), user, stack, entity).getResult();
    }

    private TypedActionResult<ItemStack> captureEntity(World world, PlayerEntity player, ItemStack stack, Entity entity) {

        // Check if entity is currently summoned by any player
        if (SummonedPetManager.isEntitySummonedByAnyPlayer(entity.getUuid())) {
            player.sendMessage(Text.translatable("item.petsinabag.pets_bag.entity_summoned"), true);
            return TypedActionResult.fail(stack);
        }

        // Check if entity is allowed to be captured
        String entityId = Registry.ENTITY_TYPE.getId(entity.getType()).toString();
        if (!PetsInABag.CONFIG.getAllowedPets().contains(entityId)) {
            player.sendMessage(Text.translatable("item.petsinabag.pets_bag.entity_not_allowed"), true);
            return TypedActionResult.fail(stack);
        }
        
        // Check if entity is tameable and owned by the player
        if (entity instanceof TameableEntity tameable) {
            if (!tameable.isTamed() || !tameable.isOwner(player)) {
                player.sendMessage(Text.translatable("item.petsinabag.pets_bag.not_owner"), true);
                return TypedActionResult.fail(stack);
            }
        }
        
        // Check if bag has space
        if (PetBagData.getPetCount(stack) >= this.maxCapacity) {
            player.sendMessage(Text.translatable("item.petsinabag.pets_bag.full"), true);
            return TypedActionResult.fail(stack);
        }
        
        // Check dimension
        String currentDimension = world.getRegistryKey().getValue().toString();
        if (!PetsInABag.CONFIG.getAllowedDimensions().contains(currentDimension)) {
            player.sendMessage(Text.translatable("item.petsinabag.pets_bag.dimension_not_allowed"), true);
            return TypedActionResult.fail(stack);
        }
        
        // Capture the entity
        if (PetBagData.addPet(stack, entity)) {
            entity.discard();
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            player.sendMessage(Text.translatable("item.petsinabag.pets_bag.captured", 
                entity.hasCustomName() ? entity.getCustomName() : entity.getType().getName()), true);
            
            return TypedActionResult.success(stack);
        }
        
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);
        // TODO: Show pet inventory UI when equipped
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onUnequip(stack, slot, entity);
        // TODO: Hide pet inventory UI when unequipped
    }
}
