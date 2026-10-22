package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lunaos.luhub.screen.WindowDrag;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Der Rezeptbuch-Knopf (Axo) richtet sich senkrecht an der Bildschirmmitte aus –
 * hier wandert er mit, wenn das Fenster nach oben oder unten verschoben ist.
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
	@ModifyExpressionValue(method = {"initButton", "lambda$initButton$0"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;getRecipeBookButtonPosition()Lnet/minecraft/client/gui/navigation/ScreenPosition;"))
	private ScreenPosition luhub$followWindow(ScreenPosition pos) {
		return new ScreenPosition(pos.x(), pos.y() + WindowDrag.shiftY((AbstractContainerScreen<?>) (Object) this));
	}
}
