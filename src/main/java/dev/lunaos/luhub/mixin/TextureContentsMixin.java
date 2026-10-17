package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Färbt eigenständige Container-Texturen (textures/gui/container/*.png) beim Laden um. */
@Mixin(TextureContents.class)
public abstract class TextureContentsMixin {
	@Inject(method = "load", at = @At("RETURN"))
	private static void luhub$themeTexture(ResourceManager resourceManager, Identifier location, CallbackInfoReturnable<TextureContents> cir) {
		if (NightTheme.shouldThemeTexture(location)) {
			NightTheme.apply(cir.getReturnValue().image(), location);
		}
	}
}
