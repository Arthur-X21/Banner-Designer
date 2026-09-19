package com.bannerdesigner.client.mixin;

import com.bannerdesigner.client.gui.BannerDesignerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.LoomScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoomScreen.class)
public abstract class LoomScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void bannerDesigner$addDesignerButton(CallbackInfo ci) {
        LoomScreen self = (LoomScreen) (Object) this;
        LoomScreenHandler handler = (LoomScreenHandler) self.getScreenHandler();

        ButtonWidget designerButton = ButtonWidget.builder(
                Text.translatable("bannerdesigner.button.open"),
                button -> MinecraftClient.getInstance().setScreen(
                        new BannerDesignerScreen(handler)
                )
        ).dimensions(8, self.height - 32, 140, 24).build();

        ((ScreenInvoker) self).bannerDesigner$addDrawableChild(designerButton);
    }
}
