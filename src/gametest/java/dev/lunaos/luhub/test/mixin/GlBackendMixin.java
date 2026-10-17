package dev.lunaos.luhub.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Nur für den automatischen Test in einer virtuellen Anzeige (Xvfb + Mesa):
 * dort gibt es keinen sRGB-fähigen GLX-Modus, daher wird diese Anforderung übersprungen.
 */
@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlBackend")
public abstract class GlBackendMixin {
	private static final int SDL_GL_FRAMEBUFFER_SRGB_CAPABLE = 22;

	@WrapOperation(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z"))
	private boolean luhubTest$skipSrgb(int attribute, int value, Operation<Boolean> original) {
		if (attribute == SDL_GL_FRAMEBUFFER_SRGB_CAPABLE && System.getenv("LUHUB_TEST_NO_SRGB") != null) {
			return true;
		}
		return original.call(attribute, value);
	}
}
