package dev.lunaos.luhub.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {
	/** Welche Leiste gerade über der Hotbar sitzt (XP, Ortung, Pferdesprung). */
	@Accessor("contextualInfoBar")
	Pair<?, ContextualBar> luhub$getContextualInfoBar();
}
