package net.brodino.petsinabag.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class PetScreen extends Screen {


    public PetScreen() {
        super(Text.literal(""));
        super.init(
                MinecraftClient.getInstance(),
                MinecraftClient.getInstance().getWindow().getScaledWidth() / 2,
                MinecraftClient.getInstance().getWindow().getScaledHeight() / 2
        );

        if (this.client.player == null) {
            client.setScreen(null);
            return;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
    }
}
