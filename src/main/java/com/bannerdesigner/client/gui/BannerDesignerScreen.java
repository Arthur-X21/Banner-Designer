package com.bannerdesigner.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.text.Text;

public class BannerDesignerScreen extends Screen {

    private final LoomScreenHandler loomHandler;

    public BannerDesignerScreen(LoomScreenHandler loomHandler) {
        super(Text.translatable("bannerdesigner.screen.title"));
        this.loomHandler = loomHandler;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int y = this.height / 4;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.back"),
                b -> this.close()
        ).dimensions(centerX - 100, y + 120, 200, 24).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // In Minecraft 1.21.11, super.render(...) already calls renderBackground internally.
        // Do NOT call this.renderBackground(...) here, or the blur will be applied twice per frame,
        // causing: java.lang.IllegalStateException: Can only blur once per frame
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
