package dev.lunaos.luhub.mixin.rei;

import dev.lunaos.luhub.theme.NightTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * REI im Nachthimmel-Theme: REI nutzt dann seine dunklen Texturen (die Lu Hub beim Laden umfärbt)
 * und helle Schrift – so bleibt alles lesbar.
 */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.config.ConfigObjectImpl", remap = false)
public abstract class ReiDarkThemeMixin {
	@Inject(method = "isUsingDarkTheme", at = @At("HEAD"), cancellable = true, require = 0)
	private void luhub$nightTheme(CallbackInfoReturnable<Boolean> cir) {
		if (NightTheme.themesRei()) {
			cir.setReturnValue(true);
		}
	}
}
