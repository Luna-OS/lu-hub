package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.theme.ThemeLabels;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Die Überschrift im Fortschritte-Fenster ist dunkelgrau – im Theme wird sie hell. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin {
	@Inject(method = "extractWindow", at = @At("HEAD"))
	private void luhub$beginLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		ThemeLabels.active = ThemeLabels.allGuis();
	}

	@Inject(method = "extractWindow", at = @At("RETURN"))
	private void luhub$endLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
		ThemeLabels.active = false;
	}
}
