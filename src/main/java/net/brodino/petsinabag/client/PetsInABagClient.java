package net.brodino.petsinabag.client;

import net.brodino.petsinabag.client.screen.PetScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class PetsInABagClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        this.registerCommand();
    }

    private void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("dio")
                            .executes(context -> {
                                MinecraftClient.getInstance().setScreen(new PetScreen());
                                return 0;
                            }
                    )
            );
        });

    }
}
