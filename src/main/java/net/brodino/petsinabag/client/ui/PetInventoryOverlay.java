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
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PetInventoryOverlay {
    private static final Identifier PET_INVENTORY_TEXTURE = new Identifier("petsinabag", "textures/gui/pet_inventory.png");
    private static final int OVERLAY_WIDTH = 26; // Single column width
    private static final int OVERLAY_HEIGHT = 166; // Same height as inventory
    private static final int SLOT_SIZE = 18;
    private static final int MAX_PETS = 9;
    private static final int PADDING = 4;
    private static final int PREVIEW_WIDTH = 50;
    private static final int PREVIEW_HEIGHT = 50;
    
    // Texture coordinates
    private static final int BACKGROUND_U = 0;
    private static final int BACKGROUND_V = 0;
    private static final int SLOT_U = 26;
    private static final int SLOT_V = 0;
    private static final int BUTTON_NORMAL_U = 0;
    private static final int BUTTON_NORMAL_V = 166;
    private static final int BUTTON_HOVERED_U = 50;
    private static final int BUTTON_HOVERED_V = 166;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_SPACING = 2;
    
    private static Set<UUID> summonedPetUUIDs = Set.of();
    private static int hoveredPetIndex = -1;
    private static int selectedPetIndex = -1;
    private static boolean showActionButtons = false;
    private static int previewPetIndex = -1; // Which pet to show preview for
    
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
        int overlayX = screenX - OVERLAY_WIDTH - 4;
        int overlayY = screenY; // Vertically centered with inventory
        
        // Set up texture rendering
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, PET_INVENTORY_TEXTURE);
        
        // Draw main background panel (single column)
        screen.drawTexture(matrices, overlayX, overlayY, BACKGROUND_U, BACKGROUND_V, OVERLAY_WIDTH, OVERLAY_HEIGHT);
        
        // Reset hoveredPetIndex each frame
        hoveredPetIndex = -1;
        
        // Draw slot backgrounds
        for (int i = 0; i < MAX_PETS; i++) {
            int slotX = overlayX + PADDING;
            int slotY = overlayY + PADDING + i * SLOT_SIZE;
            
            // Draw slot background texture
            screen.drawTexture(matrices, slotX, slotY, SLOT_U, SLOT_V, SLOT_SIZE, SLOT_SIZE);
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
                hoveredPetIndex = i;
                screen.fill(matrices, slotX, slotY, slotX + 16, slotY + 16, 0x80FFFFFF); // White highlight
                
                // Update preview to show hovered pet (overrides any selected pet)
                previewPetIndex = i;
                
                // Show tooltip
                screen.renderTooltip(matrices, Text.literal(petData.getCustomName()), mouseX, mouseY);
            }
        }
        
        // If hovering a pet, that takes priority. If not hovering but have a selected pet, show that.
        if (hoveredPetIndex == -1 && selectedPetIndex >= 0) {
            previewPetIndex = selectedPetIndex;
        }
        
        // Render action buttons first (so preview renders on top)
        if (showActionButtons && selectedPetIndex >= 0 && selectedPetIndex < pets.size()) {
            renderActionButtons(matrices, screen, overlayX, overlayY, pets.get(selectedPetIndex), selectedPetIndex, mouseX, mouseY);
        }
        
        // Render preview on top (takes priority over buttons)
        if (previewPetIndex >= 0 && previewPetIndex < pets.size()) {
            renderPetPreview(matrices, screen, pets.get(previewPetIndex), overlayX, overlayY, previewPetIndex);
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
    
    private static void renderActionButtons(MatrixStack matrices, HandledScreen<?> screen, int overlayX, int overlayY, StoredPets petData, int petIndex, int mouseX, int mouseY) {
        // Position buttons next to the selected pet slot, below the preview
        int slotY = overlayY + PADDING + petIndex * SLOT_SIZE;
        int buttonX = overlayX - PREVIEW_WIDTH - 5;
        int buttonY = slotY + PREVIEW_HEIGHT + 2; // 2px below preview
        
        // Calculate total button area height (2 buttons + spacing between them)
        int totalButtonHeight = (BUTTON_HEIGHT * 2) + BUTTON_SPACING;
        
        // Draw background for button area
        screen.fill(matrices, buttonX, buttonY, buttonX + PREVIEW_WIDTH, buttonY + totalButtonHeight, 0xFF000000);
        screen.fill(matrices, buttonX + 1, buttonY + 1, buttonX + PREVIEW_WIDTH - 1, buttonY + totalButtonHeight - 1, 0xFF373737);
        
        // Summon/Recall button
        String actionText = petData.isSummoned() ? "Recall" : "Summon";
        renderButton(matrices, screen, actionText, buttonX + (PREVIEW_WIDTH - BUTTON_WIDTH) / 2, buttonY + 2, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
        
        // Release button (with spacing)
        String releaseText = "Release";
        int releaseButtonY = buttonY + 2 + BUTTON_HEIGHT + BUTTON_SPACING;
        renderButton(matrices, screen, releaseText, buttonX + (PREVIEW_WIDTH - BUTTON_WIDTH) / 2, releaseButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
    }
    
    private static void renderPetPreview(MatrixStack matrices, HandledScreen<?> screen, StoredPets petData, int overlayX, int overlayY, int petIndex) {
        // Position preview area next to the specific pet slot
        int slotY = overlayY + PADDING + petIndex * SLOT_SIZE;
        int previewX = overlayX - PREVIEW_WIDTH - 5;
        int previewY = slotY - (PREVIEW_HEIGHT - SLOT_SIZE) / 2; // Center preview with pet slot
        
        // Draw preview background
        screen.fill(matrices, previewX, previewY, previewX + PREVIEW_WIDTH, previewY + PREVIEW_HEIGHT, 0xFF000000);
        screen.fill(matrices, previewX + 1, previewY + 1, previewX + PREVIEW_WIDTH - 1, previewY + PREVIEW_HEIGHT - 1, 0xFF373737);
        
        // Render the entity isometrically (centered in preview window)
        renderEntityPreview(matrices, screen, petData, previewX + PREVIEW_WIDTH / 2, previewY + PREVIEW_HEIGHT / 2, 20);
    }
    
    private static void renderEntityPreview(MatrixStack matrices, HandledScreen<?> screen, StoredPets petData, int x, int y, int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        try {
            // Get entity type from stored data
            Identifier entityTypeId = new Identifier(petData.getEntityTypeId());
            EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeId);
            
            // Create a temporary entity for rendering
            Entity entity = entityType.create(client.world);
            if (entity == null) return;
            
            // Load entity data from NBT
            entity.readNbt(petData.getEntityNbt());
            
            // Set up rendering matrices for isometric view
            matrices.push();
            matrices.translate(x, y, 100);
            matrices.scale(size, -size, size);
            
            // Isometric rotation (30 degrees around X-axis, 45 degrees around Y-axis)
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(30));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(45));
            
            // Set up lighting for entity rendering
            EntityRenderDispatcher entityRenderDispatcher = client.getEntityRenderDispatcher();
            VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
            
            // Render the entity
            entityRenderDispatcher.render(entity, 0, 0, 0, 0, 0, matrices, immediate, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            immediate.draw();
            
            matrices.pop();
        } catch (Exception e) {
            // If entity rendering fails, show a fallback colored square
            int color = getColorForEntityType(petData.getEntityTypeId());
            screen.fill(matrices, x - 8, y - 8, x + 8, y + 8, color);
        }
    }
    
    private static void renderSmallButton(MatrixStack matrices, HandledScreen<?> screen, String text, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Draw button background
        if (hovered) {
            screen.fill(matrices, x, y, x + width, y + height, 0xFFFFFFFF);
            screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
        } else {
            screen.fill(matrices, x, y, x + width, y + height, 0xFF000000);
            screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
        }
        
        // Draw button text centered with smaller font
        MinecraftClient client = MinecraftClient.getInstance();
        matrices.push();
        matrices.scale(0.6f, 0.6f, 1.0f);
        int scaledX = (int)((x + width / 2) / 0.6f - client.textRenderer.getWidth(text) / 2);
        int scaledY = (int)((y + height / 2 - 2) / 0.6f);
        client.textRenderer.draw(matrices, text, scaledX, scaledY, hovered ? 0xFF000000 : 0xFFFFFFFF);
        matrices.pop();
    }
    
    private static void renderButton(MatrixStack matrices, HandledScreen<?> screen, String text, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Set texture for button rendering
        RenderSystem.setShaderTexture(0, PET_INVENTORY_TEXTURE);
        
        // Draw button background texture based on state
        if (hovered) {
            screen.drawTexture(matrices, x, y, BUTTON_HOVERED_U, BUTTON_HOVERED_V, width, height);
        } else {
            screen.drawTexture(matrices, x, y, BUTTON_NORMAL_U, BUTTON_NORMAL_V, width, height);
        }
        
        // Draw button text centered
        MinecraftClient client = MinecraftClient.getInstance();
        int textX = x + (width - client.textRenderer.getWidth(text)) / 2;
        int textY = y + (height - 8) / 2;
        client.textRenderer.draw(matrices, text, textX, textY, 0xFF000000);
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
        
        // Check if click is within UI area (including preview and button areas)
        boolean clickInUIArea = false;
        
        // Check main overlay area
        if (mouseX >= overlayX && mouseX < overlayX + OVERLAY_WIDTH && mouseY >= overlayY && mouseY < overlayY + OVERLAY_HEIGHT) {
            clickInUIArea = true;
        }
        
        // Check preview/button area for selected pet
        if (selectedPetIndex >= 0) {
            int slotY = overlayY + PADDING + selectedPetIndex * SLOT_SIZE;
            int previewX = overlayX - PREVIEW_WIDTH - 5;
            int previewY = slotY - (PREVIEW_HEIGHT - SLOT_SIZE) / 2;
            int buttonY = slotY + PREVIEW_HEIGHT + 2;
            int totalButtonHeight = (BUTTON_HEIGHT * 2) + BUTTON_SPACING + 4; // +4 for padding
            
            // Check preview area
            if (mouseX >= previewX && mouseX < previewX + PREVIEW_WIDTH && mouseY >= previewY && mouseY < previewY + PREVIEW_HEIGHT) {
                clickInUIArea = true;
            }
            
            // Check button area
            if (showActionButtons && mouseX >= previewX && mouseX < previewX + PREVIEW_WIDTH && mouseY >= buttonY && mouseY < buttonY + totalButtonHeight) {
                clickInUIArea = true;
            }
        }
        
        // Check pet slot clicks (single column)
        for (int i = 0; i < Math.min(pets.size(), MAX_PETS); i++) {
            int slotX = overlayX + PADDING;
            int slotY = overlayY + PADDING + i * SLOT_SIZE;
            
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                selectedPetIndex = i;
                showActionButtons = true;
                previewPetIndex = i;
                return true;
            }
        }
        
        // Check action button clicks (below preview area)
        if (showActionButtons && selectedPetIndex >= 0 && selectedPetIndex < pets.size()) {
            StoredPets petData = pets.get(selectedPetIndex);
            
            // Calculate button positions (same as in render method)
            int slotY = overlayY + PADDING + selectedPetIndex * SLOT_SIZE;
            int buttonX = overlayX - PREVIEW_WIDTH - 5;
            int buttonY = slotY + PREVIEW_HEIGHT + 2; // 2px below preview
            int centeredButtonX = buttonX + (PREVIEW_WIDTH - BUTTON_WIDTH) / 2;
            
            // Summon/Recall button
            int actionButtonY = buttonY + 2;
            if (mouseX >= centeredButtonX && mouseX < centeredButtonX + BUTTON_WIDTH && mouseY >= actionButtonY && mouseY < actionButtonY + BUTTON_HEIGHT) {
                if (petData.isSummoned()) {
                    sendRecallPacket(selectedPetIndex);
                } else {
                    sendSummonPacket(selectedPetIndex);
                }
                showActionButtons = false;
                selectedPetIndex = -1;
                previewPetIndex = -1;
                return true;
            }
            
            // Release button
            int releaseButtonY = buttonY + 2 + BUTTON_HEIGHT + BUTTON_SPACING;
            if (mouseX >= centeredButtonX && mouseX < centeredButtonX + BUTTON_WIDTH && mouseY >= releaseButtonY && mouseY < releaseButtonY + BUTTON_HEIGHT) {
                sendReleasePacket(selectedPetIndex);
                showActionButtons = false;
                selectedPetIndex = -1;
                previewPetIndex = -1;
                return true;
            }
        }
        
        // Click outside UI area clears all state
        if (!clickInUIArea) {
            showActionButtons = false;
            selectedPetIndex = -1;
            previewPetIndex = -1;
        }
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
    

}