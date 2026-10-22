package dev.lunaos.luhub.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Lu Hub bringt die Funktionen von AppleSkin, Shulker Box Tooltip, Sorted und uku's Armor HUD selbst mit.
 * Ist eine dieser Mods trotzdem installiert, schaltet Lu Hub seine eigene Variante ab, damit nichts doppelt erscheint.
 */
public final class ExternalMods {
	public static final boolean APPLESKIN = loaded("appleskin");
	public static final boolean SHULKER_BOX_TOOLTIP = loaded("shulkerboxtooltip");
	public static final boolean SORTED = loaded("sorted");
	public static final boolean UKUS_ARMOR_HUD = loaded("ukus-armor-hud");
	/** Tutorial Tracker (Luna-OS) – wird nicht ersetzt, sondern ans Theme angepasst. */
	public static final boolean TUTORIAL_TRACKER = loaded("tutorialtracker");

	private ExternalMods() {
	}

	private static boolean loaded(String id) {
		return FabricLoader.getInstance().isModLoaded(id);
	}
}
