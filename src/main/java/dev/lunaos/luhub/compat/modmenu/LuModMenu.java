package dev.lunaos.luhub.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.lunaos.luhub.screen.LuConfigScreen;

/** Einstellungs-Knopf in der Mod-Liste von Mod Menu (optional). */
public final class LuModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return LuConfigScreen::new;
	}
}
