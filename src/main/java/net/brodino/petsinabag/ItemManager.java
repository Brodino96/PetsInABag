package net.brodino.petsinabag;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

public class ItemManager {

    public static Item PETS_BAG;

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registry.ITEM, new Identifier(PetsInABag.MOD_ID, name), item);
    }

    public static void initialize() {
        PetsInABag.LOGGER.info("Initializing items");
        ItemManager.PETS_BAG = ItemManager.register("pets_bag", new Item(new Item.Settings()
                .group(ItemGroup.TOOLS)
                .rarity(Rarity.EPIC)
                .maxCount(1)
                .fireproof()
        ));
    }
}
