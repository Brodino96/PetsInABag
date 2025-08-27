package net.brodino.petsinabag.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.brodino.petsinabag.item.data.PetBagData;
import net.brodino.petsinabag.item.data.StoredPets;
import net.brodino.petsinabag.item.ItemManager;
import net.brodino.petsinabag.network.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PetInventoryOverlay {
    private static final Identifier PETS_TEXTURE = new Identifier("minecraft", "textures/gui/container/generic_54.png");
    private static final int OVERLAY_WIDTH = 26;
    private static final int OVERLAY_HEIGHT = 166;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_COLUMN = 9;
    private static final int MAX_PETS = 9;
    private static final int PADDING = 4;
    
    private static Set<UUID> summonedPetUUIDs = Set.of();
    private static boolean showActionButtons = false;
    private static int selectedPetIndex = -1;
    
    public static void render(MatrixStack matrices, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (!(screen instanceof InventoryScreen)) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        ItemStack bagStack = getBagFromTrinkets();
        if (bagStack.isEmpty()) {
            return;
        }
        
        List<StoredPets> pets = PetBagData.getStoredPets(bagStack);
        if (pets.isEmpty()) {
            return;
        }
        
        // Calculate overlay position (left side of inventory, vertically centered)
        int screenX = (screen.width - 176) / 2;
        int screenY = (screen.height - 166) / 2;
        int overlayX = screenX - OVERLAY_WIDTH - 8;
        int overlayY = screenY; // Vertically centered with inventory
        
        // Render inventory-style background
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        // Draw inventory-style background with rounded appearance
        drawInventoryStyleBackground(matrices, screen, overlayX, overlayY, OVERLAY_WIDTH, OVERLAY_HEIGHT);
        
        // Draw slot backgrounds
        for (int i = 0; i < MAX_PETS; i++) {
            int slotX = overlayX + PADDING;
            int slotY = overlayY + PADDING + i * SLOT_SIZE;
            
            // Draw inventory-style slot
            drawInventorySlot(matrices, screen, slotX, slotY);
        }
        
        // Render pet slots (single column)
        for (int i = 0; i < Math.min(pets.size(), MAX_PETS); i++) {
            int slotX = overlayX + PADDING + 1;
            int slotY = overlayY + PADDING + i * SLOT_SIZE + 1;
            
            StoredPets petData = pets.get(i);
            
            // Highlight summoned pets
            if (petData.isSummoned()) {
                screen.fill(matrices, slotX, slotY, slotX + 16, slotY + 16, 0x8000FF00); // Green highlight
            }
            
            // Render pet "icon" (simplified)
            renderPetIcon(matrices, screen, petData, slotX, slotY);
            
            // Check if mouse is over this slot (using the actual slot area)
            int actualSlotX = overlayX + PADDING;
            int actualSlotY = overlayY + PADDING + i * SLOT_SIZE;
            if (mouseX >= actualSlotX && mouseX < actualSlotX + 18 && mouseY >= actualSlotY && mouseY < actualSlotY + 18) {
                screen.fill(matrices, slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF); // White highlight
                
                // Show tooltip
                screen.renderTooltip(matrices, Text.literal(petData.getCustomName()), mouseX, mouseY);
            }
        }
        
        // Show action buttons if a pet is selected
        if (showActionButtons && selectedPetIndex >= 0 && selectedPetIndex < pets.size()) {
            renderActionButtons(matrices, screen, overlayX, overlayY, pets.get(selectedPetIndex), mouseX, mouseY);
        }
    }
    
    private static void renderPetIcon(MatrixStack matrices, HandledScreen<?> screen, StoredPets petData, int x, int y) {
        // Render a colored icon based on entity type
        String entityType = petData.getEntityTypeId();
        int color = getColorForEntityType(entityType);
        
        // Draw main pet color
        screen.fill(matrices, x + 3, y + 3, x + 13, y + 13, color);
        
        // Add a subtle border for better visibility
        screen.fill(matrices, x + 2, y + 2, x + 14, y + 3, 0xFF000000); // Top
        screen.fill(matrices, x + 2, y + 13, x + 14, y + 14, 0xFF000000); // Bottom
        screen.fill(matrices, x + 2, y + 2, x + 3, y + 14, 0xFF000000); // Left
        screen.fill(matrices, x + 13, y + 2, x + 14, y + 14, 0xFF000000); // Right
        
        // TODO: Render actual entity models or icons
    }
    
    private static int getColorForEntityType(String entityType) {
        // Improved color mapping for different entity types
        return switch (entityType) {
            case "minecraft:cat" -> 0xFFFF8C42; // Orange
            case "minecraft:dog", "minecraft:wolf" -> 0xFF8C7853; // Brown
            case "minecraft:axolotl" -> 0xFFFF69B4; // Pink
            case "minecraft:parrot" -> 0xFF32CD32; // Lime green
            case "minecraft:rabbit" -> 0xFFDEB887; // Burlywood
            case "minecraft:fox" -> 0xFFFF4500; // Orange red
            case "minecraft:ocelot" -> 0xFFFFD700; // Gold
            case "minecraft:panda" -> 0xFF000000; // Black
            case "minecraft:polar_bear" -> 0xFFFFFFFF; // White
            default -> 0xFF808080; // Gray
        };
    }
    
    private static void renderActionButtons(MatrixStack matrices, HandledScreen<?> screen, int overlayX, int overlayY, StoredPets petData, int mouseX, int mouseY) {
        // Position buttons to the right of the column
        int buttonX = overlayX + OVERLAY_WIDTH + 5;
        
        // Summon/Recall button
        String actionText = petData.isSummoned() ? "Recall" : "Summon";
        int actionButtonY = overlayY + 20;
        renderButton(matrices, screen, actionText, buttonX, actionButtonY, 50, 16, mouseX, mouseY);
        
        // Release button
        String releaseText = "Release";
        int releaseButtonY = overlayY + 40;
        renderButton(matrices, screen, releaseText, buttonX, releaseButtonY, 50, 16, mouseX, mouseY);
    }
    
    private static void renderButton(MatrixStack matrices, HandledScreen<?> screen, String text, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Draw inventory-style button
        if (hovered) {
            // Hovered state - lighter
            screen.fill(matrices, x, y, x + width, y + height, 0xFFFFFFFF);
            screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE0E0E0);
        } else {
            // Normal state - like inventory background
            screen.fill(matrices, x, y, x + width, y + height, 0xFFC6C6C6);
            screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
        }
        
        // Black outline
        screen.fill(matrices, x - 1, y - 1, x + width + 1, y, 0xFF000000); // Top
        screen.fill(matrices, x - 1, y + height, x + width + 1, y + height + 1, 0xFF000000); // Bottom
        screen.fill(matrices, x - 1, y - 1, x, y + height + 1, 0xFF000000); // Left
        screen.fill(matrices, x + width, y - 1, x + width + 1, y + height + 1, 0xFF000000); // Right
        
        // Button depth effect
        if (!hovered) {
            screen.fill(matrices, x, y, x + width, y + 1, 0xFF8B8B8B); // Top shadow
            screen.fill(matrices, x, y, x + 1, y + height, 0xFF8B8B8B); // Left shadow
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int textX = x + (width - client.textRenderer.getWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        screen.drawCenteredText(matrices, client.textRenderer, text, textX + width / 2, textY, 0xFF000000);
    }
    
    public static boolean handleClick(HandledScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!(screen instanceof InventoryScreen)) {
            return false;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        
        ItemStack bagStack = getBagFromTrinkets();
        if (bagStack.isEmpty()) {
            return false;
        }
        
        List<StoredPets> pets = PetBagData.getStoredPets(bagStack);
        if (pets.isEmpty()) {
            return false;
        }
        
        // Calculate overlay position
        int screenX = (screen.width - 176) / 2;
        int screenY = (screen.height - 166) / 2;
        int overlayX = screenX - OVERLAY_WIDTH - 8;
        int overlayY = screenY;
        
        // Check pet slot clicks (single column)
        for (int i = 0; i < Math.min(pets.size(), MAX_PETS); i++) {
            int slotX = overlayX + PADDING;
            int slotY = overlayY + PADDING + i * SLOT_SIZE;
            
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                selectedPetIndex = i;
                showActionButtons = true;
                return true;
            }
        }
        
        // Check action button clicks
        if (showActionButtons && selectedPetIndex >= 0 && selectedPetIndex < pets.size()) {
            int buttonX = overlayX + OVERLAY_WIDTH + 5;
            StoredPets petData = pets.get(selectedPetIndex);
            
            // Summon/Recall button
            int actionButtonY = overlayY + 20;
            if (mouseX >= buttonX && mouseX < buttonX + 50 && mouseY >= actionButtonY && mouseY < actionButtonY + 16) {
                if (petData.isSummoned()) {
                    sendRecallPacket(selectedPetIndex);
                } else {
                    sendSummonPacket(selectedPetIndex);
                }
                showActionButtons = false;
                selectedPetIndex = -1;
                return true;
            }
            
            // Release button
            int releaseButtonY = overlayY + 40;
            if (mouseX >= buttonX && mouseX < buttonX + 50 && mouseY >= releaseButtonY && mouseY < releaseButtonY + 16) {
                sendReleasePacket(selectedPetIndex);
                showActionButtons = false;
                selectedPetIndex = -1;
                return true;
            }
        }
        
        // Click elsewhere closes action buttons
        showActionButtons = false;
        selectedPetIndex = -1;
        return false;
    }
    
    private static void sendSummonPacket(int petIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(petIndex);
        ClientPlayNetworking.send(NetworkHandler.SUMMON_PET_PACKET, buf);
    }
    
    private static void sendRecallPacket(int petIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(petIndex);
        ClientPlayNetworking.send(NetworkHandler.RECALL_PET_PACKET, buf);
    }
    
    private static void sendReleasePacket(int petIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(petIndex);
        ClientPlayNetworking.send(NetworkHandler.RELEASE_PET_PACKET, buf);
    }
    
    private static ItemStack getBagFromTrinkets() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        
        // Check main hand and offhand first
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() == ItemManager.PETS_BAG) {
            return mainHand;
        }
        
        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() == ItemManager.PETS_BAG) {
            return offHand;
        }
        
        // TODO: Check trinket slots when API is properly accessible
        return ItemStack.EMPTY;
    }
    
    public static void updateSummonedPets(Set<UUID> newSummonedPets) {
        summonedPetUUIDs = newSummonedPets;
    }
    
    private static void drawInventoryStyleBackground(MatrixStack matrices, HandledScreen<?> screen, int x, int y, int width, int height) {
        // Main background (light gray/white)
        screen.fill(matrices, x, y, x + width, y + height, 0xFFC6C6C6);
        
        // Black outline
        screen.fill(matrices, x - 1, y - 1, x + width + 1, y, 0xFF000000); // Top
        screen.fill(matrices, x - 1, y + height, x + width + 1, y + height + 1, 0xFF000000); // Bottom
        screen.fill(matrices, x - 1, y - 1, x, y + height + 1, 0xFF000000); // Left
        screen.fill(matrices, x + width, y - 1, x + width + 1, y + height + 1, 0xFF000000); // Right
        
        // Inner shadow/depth effect (darker gray at top and left)
        screen.fill(matrices, x, y, x + width, y + 1, 0xFF8B8B8B); // Top shadow
        screen.fill(matrices, x, y, x + 1, y + height, 0xFF8B8B8B); // Left shadow
        
        // Lighter highlight at bottom and right
        screen.fill(matrices, x, y + height - 1, x + width, y + height, 0xFFFFFFFF); // Bottom highlight
        screen.fill(matrices, x + width - 1, y, x + width, y + height, 0xFFFFFFFF); // Right highlight
    }
    
    private static void drawInventorySlot(MatrixStack matrices, HandledScreen<?> screen, int x, int y) {
        // Slot background (darker gray)
        screen.fill(matrices, x, y, x + 18, y + 18, 0xFF8B8B8B);
        
        // Slot inner area (darker)
        screen.fill(matrices, x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        
        // Slot highlight borders
        screen.fill(matrices, x, y, x + 18, y + 1, 0xFF373737); // Top
        screen.fill(matrices, x, y, x + 1, y + 18, 0xFF373737); // Left
        screen.fill(matrices, x, y + 17, x + 18, y + 18, 0xFFFFFFFF); // Bottom
        screen.fill(matrices, x + 17, y, x + 18, y + 18, 0xFFFFFFFF); // Right
    }
}