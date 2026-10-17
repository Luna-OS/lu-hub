package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Färbt eigenständige GUI-Texturen (Container, Menü-Hintergründe, Fortschritte ...) beim Laden um. */
@Mixin(TextureContents.class)
public abstract class TextureContentsMixin {
	@Inject(method = "load", at = @At("RETURN"))
	private static void luhub$themeTexture(ResourceManager resourceManager, Identifier location, CallbackInfoReturnable<TextureContents> cir) {
		NightTheme.Kind kind = NightTheme.textureKind(location);
		if (kind != null) {
			NightTheme.generation++;
			NightTheme.apply(cir.getReturnValue().image(), location, kind);
		}
	}
}
