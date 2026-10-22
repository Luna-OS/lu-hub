package dev.lunaos.luhub.compat;

import dev.lunaos.luhub.LuHub;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Tutorial Tracker (Luna-OS): dessen Fenster zeichnet die Mod selbst mit den Farben aus {@code McStyle}.
 * Im Nachthimmel-Theme setzt Lu Hub diese Farben auf die Theme-Palette; ohne Theme wird das Original
 * (hell oder dunkel) wiederhergestellt.
 */
public final class TutorialTrackerCompat {
	private static final String STYLE_CLASS = "com.lunaos.tutorialtracker.gui.McStyle";
	/** Package der Mod – deren Container-Knopf richtet sich an der Fenstermitte aus. */
	public static final String PACKAGE = "com.lunaos.tutorialtracker.";

	private static Map<String, Field> fields;
	private static Method applyTheme;
	private static Method isDark;
	private static boolean themed;
	private static boolean failed;

	private TutorialTrackerCompat() {
	}

	/** Jeden Tick: Farben setzen bzw. zurücksetzen (die Mod kann sie jederzeit selbst umstellen). */
	public static void tick() {
		if (!ExternalMods.TUTORIAL_TRACKER || failed) {
			return;
		}
		try {
			if (fields == null) {
				Class<?> style = Class.forName(STYLE_CLASS);
				fields = new HashMap<>();
				for (String name : new String[]{"PANEL", "PANEL_LIGHT", "PANEL_SHADOW", "SLOT", "SLOT_DARK", "SLOT_LIGHT",
					"LABEL", "LABEL_DISABLED", "DARK_GREEN", "DARK_GOLD", "DARK_RED", "PREVIEW"}) {
					fields.put(name, style.getField(name));
				}
				applyTheme = style.getMethod("applyTheme", boolean.class);
				isDark = style.getMethod("isDark");
			}
			LuConfig c = LuConfig.get();
			boolean wanted = c.themeEnabled && c.themeAllGuis;
			if (wanted) {
				set("PANEL", c.colorBackground);
				set("PANEL_LIGHT", c.colorHighlight);
				set("PANEL_SHADOW", c.colorShadow);
				set("SLOT", c.colorSlot);
				set("SLOT_DARK", c.colorBorder);
				set("SLOT_LIGHT", NightTheme.lerp(c.colorSlot, c.colorHighlight, 0.5f));
				set("LABEL", c.colorLabel);
				set("LABEL_DISABLED", NightTheme.lerp(c.colorLabel, c.colorBackground, 0.55f));
				set("DARK_GREEN", 0xFF7CFF9A);
				set("DARK_GOLD", 0xFFFFD27F);
				set("DARK_RED", 0xFFFF7A8A);
				set("PREVIEW", 0xFF000000 | c.colorSkyBottom);
				themed = true;
			} else if (themed) {
				applyTheme.invoke(null, (boolean) isDark.invoke(null));
				themed = false;
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			failed = true;
			LuHub.LOGGER.warn("Tutorial-Tracker-Farben konnten nicht angepasst werden", e);
		}
	}

	private static void set(String name, int value) throws IllegalAccessException {
		fields.get(name).setInt(null, value);
	}
}
