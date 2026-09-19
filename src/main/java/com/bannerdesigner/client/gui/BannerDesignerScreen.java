package com.bannerdesigner.client.gui;

import com.bannerdesigner.client.preset.PresetEntry;
import com.bannerdesigner.client.preset.PresetManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.text.Text;

import java.util.List;

public class BannerDesignerScreen extends Screen {

    private static final int TOP_MARGIN = 70;
    private static final int BOTTOM_MARGIN = 70;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_SPACING = 4;

    private final LoomScreenHandler loomHandler;

    public BannerDesignerScreen(LoomScreenHandler loomHandler) {
        super(Text.translatable("bannerdesigner.screen.title"));
        this.loomHandler = loomHandler;
    }

    @Override
    protected void init() {
        super.init();

        PresetManager.reload();
        List<PresetEntry> presets = PresetManager.snapshot();

        int buttonWidth = Math.min(280, this.width - 40);
        int buttonX = (this.width - buttonWidth) / 2;

        int availableHeight = this.height - TOP_MARGIN - BOTTOM_MARGIN;
        int perButton = BUTTON_HEIGHT + BUTTON_SPACING;
        int maxVisible = Math.max(1, availableHeight / perButton);
        int visibleCount = Math.min(presets.size(), maxVisible);

        for (int i = 0; i < visibleCount; i++) {
            PresetEntry preset = presets.get(i);
            int y = TOP_MARGIN + i * perButton;

            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(preset.name()),
                    b -> onPresetSelected(preset)
            ).dimensions(buttonX, y, buttonWidth, BUTTON_HEIGHT).build();

            this.addDrawableChild(button);
        }

        int bottomY = this.height - 52;

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
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(
                    Text.translatable("bannerdesigner.message.preset_selected", preset.name()),
                    false
            );
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        List<PresetEntry> presets = PresetManager.snapshot();
        Text info = presets.isEmpty()
                ? Text.translatable("bannerdesigner.info.no_presets")
                : Text.translatable("bannerdesigner.info.presets_found", presets.size());

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                info,
                this.width / 2,
                42,
                0xAAAAAA
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
