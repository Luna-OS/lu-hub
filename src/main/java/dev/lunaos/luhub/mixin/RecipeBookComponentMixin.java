package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.screen.WindowDrag;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fenster mit Rezeptbuch berechnen ihre X-Position neu – die gespeicherte Verschiebung gilt auch dort. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	@Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
	private void luhub$offsetX(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(WindowDrag.offsetLeft(cir.getReturnValue(), width, imageWidth));
	}
}
