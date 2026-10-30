package dev.lunaos.luhub.mixin.rei;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Die beiden Kopfleisten im REI-Rezeptfenster (Kategorie, Seite) sind ein festes Grau – im Theme die Slot-Farbe. */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen", remap = false)
public abstract class ReiHeaderStripesMixin {
	@ModifyConstant(method = "*", constant = @Constant(intValue = 0xFF404040), require = 0)
	private static int luhub$themedStripes(int original) {
		return NightTheme.themesRei() ? LuConfig.get().colorSlot | 0xFF000000 : original;
	}
}
