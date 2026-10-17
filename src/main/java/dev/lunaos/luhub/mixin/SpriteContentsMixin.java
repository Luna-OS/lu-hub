package dev.lunaos.luhub.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lunaos.luhub.compat.ShulkerBoxTooltipCompat;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Färbt GUI-Sprites (Slots, Knöpfe, Tooltips, Hotbar, Rezeptbuch ...) beim Laden ins Nachthimmel-Theme um. */
@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin {
	@Shadow
	@Final
	private Identifier name;

	@Shadow
	@Final
	private NativeImage originalImage;

	@Inject(
		method = "<init>(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/resources/metadata/animation/FrameSize;Lcom/mojang/blaze3d/platform/NativeImage;Ljava/util/Optional;Ljava/util/List;Ljava/util/Optional;)V",
		at = @At("RETURN")
	)
	private void luhub$themeSprite(CallbackInfo ci) {
		if (this.name.equals(ShulkerBoxTooltipCompat.WINDOW_SPRITE) && ShulkerBoxTooltipCompat.themed()) {
			NightTheme.apply(this.originalImage, this.name.hashCode(), NightTheme.Kind.PANEL, ShulkerBoxTooltipCompat.neutralPalette());
			return;
		}
		NightTheme.Kind kind = NightTheme.spriteKind(this.name);
		if (kind != null) {
			NightTheme.apply(this.originalImage, this.name, kind);
		}
	}
}
