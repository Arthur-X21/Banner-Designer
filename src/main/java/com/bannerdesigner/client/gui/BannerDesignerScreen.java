package com.bannerdesigner.client.gui;

import com.bannerdesigner.client.analysis.AnalysisResult;
import com.bannerdesigner.client.analysis.ColorAnalyzer;
import com.bannerdesigner.client.banner.BannerDefinition;
import com.bannerdesigner.client.banner.BannerRenderer2D;
import com.bannerdesigner.client.image.ImageLoader;
import com.bannerdesigner.client.image.ImageTexture;
import com.bannerdesigner.client.preset.PresetEntry;
import com.bannerdesigner.client.preset.PresetManager;
import com.bannerdesigner.client.solver.BannerCandidate;
import com.bannerdesigner.client.solver.SimpleSolver;
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
    private static final int COLOR_GRAY  = 0xFFAAAAAA;
    private static final int COLOR_GREEN = 0xFF55FF55;

    private static final int SIDEBAR_X = 10;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int SIDEBAR_TOP = 45;
    private static final int PRESET_BUTTON_HEIGHT = 18;
    private static final int PRESET_BUTTON_SPACING = 3;

    private static final int IMAGE_X = 175;
    private static final int PREVIEW_TOP = 45;
    private static final int PREVIEW_BOTTOM = 55;
    private static final int PANEL_GAP = 10;

    private final LoomScreenHandler loomHandler;
    private ImageTexture currentTexture;
    private ImageTexture bannerTexture;
    private BufferedImage currentImage;
    private BannerDefinition currentBanner;
    private double currentScore;
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

        int availableHeight = this.height - SIDEBAR_TOP - 90;
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

        int analyzeY = this.height - 82;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.analyze"),
                b -> onAnalyze()
        ).dimensions(SIDEBAR_X, analyzeY, SIDEBAR_WIDTH, 20).build());

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
        destroyTextures();

        BufferedImage img = ImageLoader.load(preset.path());
        if (img == null) {
            this.statusMessage = "Failed to load: " + preset.name();
        } else {
            this.currentImage = img;
            this.currentTexture = ImageTexture.fromBufferedImage(preset.name(), img);
            this.statusMessage = "Loaded: " + preset.name()
                    + " (" + img.getWidth() + "x" + img.getHeight() + ")";
        }

        sendChat(this.statusMessage);
    }

    private void onAnalyze() {
        if (this.currentImage == null) {
            sendChat("Select a preset first.");
            return;
        }

        try {
            AnalysisResult analysis = ColorAnalyzer.analyze(this.currentImage);
            List<BannerCandidate> candidates = SimpleSolver.solve(this.currentImage, analysis, 1);
            if (candidates.isEmpty()) {
                sendChat("No candidates found.");
                return;
            }
            BannerCandidate best = candidates.get(0);
            this.currentBanner = best.definition();
            this.currentScore = best.score();

            if (this.bannerTexture != null) {
                this.bannerTexture.close();
                this.bannerTexture = null;
            }
            BufferedImage rendered = BannerRenderer2D.render(this.currentBanner);
            this.bannerTexture = ImageTexture.fromBufferedImage("banner_preview", rendered);

            sendChat(String.format("Analyzed. Match: %.1f%%", this.currentScore * 100.0));
        } catch (Exception e) {
            sendChat("Analysis failed: " + e.getMessage());
        }
    }

    private void sendChat(String msg) {
        if (this.client != null && this.client.player != null) {
            this.client.player.sendMessage(Text.literal(msg), false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 12, COLOR_WHITE
        );

        List<PresetEntry> presets = PresetManager.snapshot();
        Text info = presets.isEmpty()
                ? Text.translatable("bannerdesigner.info.no_presets")
                : Text.translatable("bannerdesigner.info.presets_found", presets.size());
        context.drawCenteredTextWithShadow(this.textRenderer, info, this.width / 2, 26, COLOR_GRAY);

        int availableW = this.width - IMAGE_X - 10;
        int availableH = this.height - PREVIEW_TOP - PREVIEW_BOTTOM;
        int halfW = (availableW - PANEL_GAP) / 2;

        int leftX = IMAGE_X;
        int rightX = IMAGE_X + halfW + PANEL_GAP;

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Original"), leftX, PREVIEW_TOP - 12, COLOR_GRAY);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal("Banner"), rightX, PREVIEW_TOP - 12, COLOR_GRAY);

        if (this.currentTexture != null) {
            drawFitted(context, this.currentTexture, leftX, PREVIEW_TOP, halfW, availableH);
        }

        if (this.bannerTexture != null) {
            drawFitted(context, this.bannerTexture, rightX, PREVIEW_TOP, halfW, availableH);
            Text score = Text.literal(String.format("Match: %.1f%%", this.currentScore * 100.0));
            context.drawCenteredTextWithShadow(this.textRenderer, score,
                    rightX + halfW / 2, PREVIEW_TOP + availableH + 4, COLOR_GREEN);
        }
    }

    private void drawFitted(DrawContext context, ImageTexture tex,
                             int areaX, int areaY, int areaW, int areaH) {
        int texW = tex.width();
        int texH = tex.height();
        float scale = Math.min((float) areaW / texW, (float) areaH / texH);
        if (scale > 1.0f) scale = 1.0f;
        int drawW = Math.max(1, (int) (texW * scale));
        int drawH = Math.max(1, (int) (texH * scale));
        int drawX = areaX + (areaW - drawW) / 2;
        int drawY = areaY + (areaH - drawH) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                tex.identifier(),
                drawX, drawY,
                0.0f, 0.0f,
                drawW, drawH,
                texW, texH
        );
    }

    private void destroyTextures() {
        if (this.currentTexture != null) {
            this.currentTexture.close();
            this.currentTexture = null;
        }
        if (this.bannerTexture != null) {
            this.bannerTexture.close();
            this.bannerTexture = null;
        }
    }

    @Override
    public void removed() {
        super.removed();
        destroyTextures();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
