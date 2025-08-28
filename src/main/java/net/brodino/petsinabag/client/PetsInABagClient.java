package net.brodino.petsinabag.client;

// import net.brodino.petsinabag.client.screen.PetScreen; // Removed - using overlay instead
import net.brodino.petsinabag.client.ui.PetInventoryOverlay;
import net.brodino.petsinabag.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PetsInABagClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        this.registerNetworking();
        this.registerScreenEvents();
    }
    
    private void registerNetworking() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_SUMMONED_PETS_PACKET, (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            Set<UUID> summonedPets = new HashSet<>();
            
            for (int i = 0; i < count; i++) {
                summonedPets.add(buf.readUuid());
            }
            
            client.execute(() -> {
                PetInventoryOverlay.updateSummonedPets(summonedPets);
            });
        });
    }
    
    private void registerScreenEvents() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                // Register render event for pet overlay
                ScreenEvents.afterRender(screen).register((screen1, matrices, mouseX, mouseY, tickDelta) -> {
                    if (screen1 instanceof HandledScreen<?> handledScreen) {
                        PetInventoryOverlay.render(matrices, handledScreen, mouseX, mouseY);
                    }
                });
                
                // Register mouse click event
                ScreenMouseEvents.afterMouseClick(screen).register((screen1, mouseX, mouseY, button) -> {
                    if (screen1 instanceof HandledScreen<?> handledScreen) {
                        PetInventoryOverlay.handleClick(handledScreen, mouseX, mouseY, button);
                    }
                });
            }
        });
    }
}
