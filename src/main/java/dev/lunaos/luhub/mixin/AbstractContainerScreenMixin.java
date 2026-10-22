package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lunaos.luhub.screen.WindowDrag;
import dev.lunaos.luhub.theme.ThemeLabels;
import dev.lunaos.luhub.theme.ThemedLabelScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Markiert, wann die Container-Überschriften ("Inventar", "Truhe" ...) gezeichnet werden. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	/** Gespeicherte Fensterposition anwenden (Drag &amp; Drop). */
	@Inject(method = "init", at = @At("RETURN"))
	private void luhub$applyWindowOffset(CallbackInfo ci) {
		WindowDrag.applyOffset((AbstractContainerScreen<?>) (Object) this);
	}

	@WrapOperation(
		method = "extractContents",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V")
	)
	private void luhub$markLabels(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, Operation<Void> original) {
		ThemeLabels.active = true;
		ThemeLabels.override = screen instanceof ThemedLabelScreen themed ? themed.luhub$labelColor() : 0;
		try {
			original.call(screen, graphics, mouseX, mouseY);
		} finally {
			ThemeLabels.active = false;
			ThemeLabels.override = 0;
		}
	}
}
