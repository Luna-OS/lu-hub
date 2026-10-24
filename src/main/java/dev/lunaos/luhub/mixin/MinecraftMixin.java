package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Kein Pausemenü mehr, nur weil man aus dem Spiel raustabbt. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "pauseIfInactive", at = @At("HEAD"), cancellable = true)
	private void luhub$noAutoPause(CallbackInfo ci) {
		if (LuConfig.get().noAutoPause) {
			ci.cancel();
		}
	}
}
