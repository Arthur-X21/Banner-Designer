package com.bannerdesigner.client.gui;

import com.bannerdesigner.client.image.ImageLoader;
import com.bannerdesigner.client.image.ImageTexture;
import com.bannerdesigner.client.preset.PresetEntry;
import com.bannerdesigner.client.preset.PresetManager;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.text.Text;

import java.awt.image.BufferedImage;
import java.util.List;

public class BannerDesignerScreen extends Screen {

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;

    private static final int SIDEBAR_X = 10;
    private static final int SIDEBAR_WIDTH = 160;
    private static final int SIDEBAR_TOP = 45;
    private static final int PRESET_BUTTON_HEIGHT = 18;
    private static final int PRESET_BUTTON_SPACING = 3;

    private static final int PREVIEW_X = 180;
    private static final int PREVIEW_TOP = 45;
    private static final int PREVIEW_BOTTOM_MARGIN = 60;

    private final LoomScreenHandler loomHandler;
    private ImageTexture currentTexture;
    private String statusMessage;

    public BannerDesignerScreen(LoomScreenHandler loomHandler) {
        super(Text.translatable("bannerdesigner.screen.title"));
        this.loomHandler = loomHandler;
    }

    @Override
    protected void init() {
        super.init();

        PresetManager.reload();
        List<PresetEntry> presets = PresetManager.snapshot();

        int availableHeight = this.height - SIDEBAR_TOP - 60;
        int perButton = PRESET_BUTTON_HEIGHT + PRESET_BUTTON_SPACING;
        int maxVisible = Math.max(1, availableHeight / perButton);
        int visibleCount = Math.min(presets.size(), maxVisible);

        for (int i = 0; i < visibleCount; i++) {
            PresetEntry preset = presets.get(i);
            int y = SIDEBAR_TOP + i * perButton;

            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(preset.name()),
                    b -> onPresetSelected(preset)
            ).dimensions(SIDEBAR_X, y, SIDEBAR_WIDTH, PRESET_BUTTON_HEIGHT).build();

            this.addDrawableChild(button);
        }

        int bottomY = this.height - 30;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.reload"),
                b -> this.clearAndInit()
        ).dimensions(this.width / 2 - 150, bottomY, 140, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.back"),
                b -> this.close()
        ).dimensions(this.width / 2 + 10, bottomY, 140, 20).build());
    }

    private void onPresetSelected(PresetEntry preset) {
        if (this.currentTexture != null) {
            this.currentTexture.close();
            this.currentTexture = null;
        }

        BufferedImage img = ImageLoader.load(preset.path());
        if (img == null) {
            this.statusMessage = "Failed to load: " + preset.name();
        } else {
            int availableW = this.width - PREVIEW_X - 10;
            int availableH = this.height - PREVIEW_TOP - PREVIEW_BOTTOM_MARGIN;
            this.currentTexture = ImageTexture.fromBufferedImage(
                    preset.name(), img, availableW, availableH
            );
            this.statusMessage = "Loaded: " + preset.name()
                    + " (" + img.getWidth() + "x" + img.getHeight() + ")";
        }

        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal(this.statusMessage), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 15, COLOR_WHITE
        );

        List<PresetEntry> presets = PresetManager.snapshot();
        Text info = presets.isEmpty()
                ? Text.translatable("bannerdesigner.info.no_presets")
                : Text.translatable("bannerdesigner.info.presets_found", presets.size());

        context.drawCenteredTextWithShadow(
                this.textRenderer, info, this.width / 2, 28, COLOR_GRAY
        );

        if (this.currentTexture != null) {
            drawPreview(context);
        }
    }

    private void drawPreview(DrawContext context) {
        int availableW = this.width - PREVIEW_X - 10;
        int availableH = this.height - PREVIEW_TOP - PREVIEW_BOTTOM_MARGIN;

        int texW = this.currentTexture.width();
        int texH = this.currentTexture.height();

        int drawX = PREVIEW_X + (availableW - texW) / 2;
        int drawY = PREVIEW_TOP + (availableH - texH) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                this.currentTexture.identifier(),
                drawX, drawY,
                0.0f, 0.0f,
                texW, texH,
                texW, texH
        );

        Text dims = Text.literal(texW + " x " + texH);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                dims,
                drawX + texW / 2,
                drawY + texH + 4,
                COLOR_WHITE
        );
    }

    @Override
    public void removed() {
        super.removed();
        if (this.currentTexture != null) {
            this.currentTexture.close();
            this.currentTexture = null;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
