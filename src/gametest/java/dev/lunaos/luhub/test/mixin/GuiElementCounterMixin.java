package dev.lunaos.luhub.test.mixin;

import dev.lunaos.luhub.test.GuiElementCounter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nur im Test: zählt die erzeugten Rechteck- und Textur-Elemente, um alt und neu zu vergleichen. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiElementCounterMixin {
	@Inject(method = {"innerFill", "innerBlit"}, at = @At("HEAD"))
	private void luhubtest$count(CallbackInfo ci) {
		GuiElementCounter.elements++;
	}
}
