package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sternenhimmel statt grauem Schleier hinter Inventar, Truhen, Öfen usw. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void luhub$nightSky(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		LuConfig config = LuConfig.get();
		Screen self = (Screen) (Object) this;
		if (config.themeEnabled && config.nightBackground && self instanceof AbstractContainerScreen) {
			ThemeDecor.extractNightSky(graphics, self.width, self.height);
			ci.cancel();
		}
	}
}
