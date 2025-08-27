package net.brodino.petsinabag;

import net.brodino.petsinabag.config.Config;
import net.brodino.petsinabag.item.ItemManager;
import net.brodino.petsinabag.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetsInABag implements ModInitializer {

    public static final String MOD_ID = "petsinabag";
    public static final Logger LOGGER = LoggerFactory.getLogger(PetsInABag.MOD_ID);
    public static final Config CONFIG = new Config();
    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing PetsInABag mod");
        
        ItemManager.initialize();
        NetworkHandler.registerServerPackets();
        SummonedPetManager.initialize();
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PetsInABag.SERVER = server;
        });
        
        LOGGER.info("PetsInABag mod initialized successfully");
    }
}
