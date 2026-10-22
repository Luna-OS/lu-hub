package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.theme.ThemeDecor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Titelbildschirm: Sternenhimmel-Schleier und Mond über dem Panorama, unter Logo und Knöpfen. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
	@Inject(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", shift = At.Shift.AFTER)
	)
	private void luhub$nightSky(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		TitleScreen self = (TitleScreen) (Object) this;
		ThemeDecor.extractTitleSky(graphics, self.width, self.height);
	}
}
