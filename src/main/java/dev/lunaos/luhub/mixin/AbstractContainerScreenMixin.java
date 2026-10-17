package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lunaos.luhub.theme.ThemeLabels;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Markiert, wann die Container-Überschriften ("Inventar", "Truhe" ...) gezeichnet werden. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@WrapOperation(
		method = "extractContents",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V")
	)
	private void luhub$markLabels(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, Operation<Void> original) {
		ThemeLabels.active = true;
		try {
			original.call(screen, graphics, mouseX, mouseY);
		} finally {
			ThemeLabels.active = false;
		}
	}
}
