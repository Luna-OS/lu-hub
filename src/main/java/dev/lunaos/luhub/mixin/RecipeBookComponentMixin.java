package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.lunaos.luhub.screen.WindowDrag;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Verschobene Fenster mit Rezeptbuch: das Rezeptbuch samt Reitern wandert mit dem Fenster mit. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	@Shadow
	private boolean widthTooNarrow;

	@Shadow
	public abstract boolean isVisible();

	private boolean luhub$besideWindow() {
		return isVisible() && !widthTooNarrow;
	}

	@ModifyReturnValue(method = "updateScreenPosition", at = @At("RETURN"))
	private int luhub$offsetX(int leftPos) {
		return WindowDrag.offsetLeft(leftPos, luhub$besideWindow());
	}

	@ModifyReturnValue(method = "getXOrigin", at = @At("RETURN"))
	private int luhub$originX(int x) {
		return x + WindowDrag.recipeBookShift(luhub$besideWindow())[0];
	}

	@ModifyReturnValue(method = "getYOrigin", at = @At("RETURN"))
	private int luhub$originY(int y) {
		return y + WindowDrag.recipeBookShift(luhub$besideWindow())[1];
	}

	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 0)
	private int luhub$tabX(int x) {
		return x + WindowDrag.recipeBookShift(luhub$besideWindow())[0];
	}

	@ModifyVariable(method = "updateTabs", at = @At("STORE"), ordinal = 1)
	private int luhub$tabY(int y) {
		return y + WindowDrag.recipeBookShift(luhub$besideWindow())[1];
	}
}
