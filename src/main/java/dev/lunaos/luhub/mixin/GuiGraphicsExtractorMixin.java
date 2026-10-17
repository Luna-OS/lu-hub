package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.theme.ThemeLabels;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hellt die dunkelgrauen Container-Überschriften auf, damit sie auf dem Nachthimmel lesbar sind. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@ModifyVariable(
		method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 2
	)
	private int luhub$recolorLabel(int color) {
		return ThemeLabels.recolor(color);
	}
}
