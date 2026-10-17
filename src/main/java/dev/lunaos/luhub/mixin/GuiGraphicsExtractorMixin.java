package dev.lunaos.luhub.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.lunaos.luhub.axo.AxoRenderer;
import dev.lunaos.luhub.compat.ShulkerBoxTooltipCompat;
import dev.lunaos.luhub.theme.ThemeLabels;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Unique
	private static boolean luhub$retinting;

	/** Hellt die dunkelgrauen Überschriften auf, damit sie auf dem Nachthimmel lesbar sind. */
	@ModifyVariable(
		method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 2
	)
	private int luhub$recolorLabel(int color) {
		return ThemeLabels.recolor(color);
	}

	/** Überall, wo das grüne Rezeptbuch gezeichnet wird (Knöpfe, Toasts), erscheint stattdessen Axo. */
	@Inject(
		method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void luhub$axoInsteadOfBook(RenderPipeline pipeline, Identifier location, int x, int y, int width, int height, int color, CallbackInfo ci) {
		GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
		if (AxoRenderer.replaceBookSprite(self, location, x, y, width, height)) {
			ci.cancel();
			return;
		}
		if (!luhub$retinting && location.equals(ShulkerBoxTooltipCompat.WINDOW_SPRITE) && ShulkerBoxTooltipCompat.themed()) {
			// Shulker Box Tooltip: Fensterfarbe so umrechnen, dass es wie das eingefärbte Nachthimmel-Theme aussieht
			luhub$retinting = true;
			try {
				self.blitSprite(pipeline, location, x, y, width, height, ShulkerBoxTooltipCompat.tint(color));
			} finally {
				luhub$retinting = false;
			}
			ci.cancel();
		}
	}
}
