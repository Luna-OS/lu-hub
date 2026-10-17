package dev.lunaos.luhub.theme;

import dev.lunaos.luhub.config.LuConfig;

/** Ersetzt die Vanilla-Farbe der Container-Überschriften (#404040) durch die Theme-Farbe. */
public final class ThemeLabels {
	private static final int VANILLA_LABEL = 0xFF404040;
	/** true, während ein Container-Screen seine Überschriften zeichnet. */
	public static boolean active;

	private ThemeLabels() {
	}

	public static int recolor(int color) {
		if (active && color == VANILLA_LABEL && LuConfig.get().themeEnabled) {
			return LuConfig.get().colorLabel;
		}
		return color;
	}
}
