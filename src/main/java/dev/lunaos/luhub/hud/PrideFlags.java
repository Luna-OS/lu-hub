package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.theme.NightTheme;

import java.util.EnumMap;
import java.util.Map;

/** Streifenfarben der Pride-Flaggen (von oben nach unten). */
public final class PrideFlags {
	private static final Map<BarStyle, int[]> STRIPES = new EnumMap<>(BarStyle.class);

	static {
		for (BarStyle style : BarStyle.values()) {
			STRIPES.put(style, create(style));
		}
	}

	private PrideFlags() {
	}

	/** Streifen der Flagge – das Array wird geteilt und darf nicht verändert werden. */
	public static int[] stripes(BarStyle style) {
		return STRIPES.get(style);
	}

	private static int[] create(BarStyle style) {
		return switch (style) {
			case RAINBOW -> new int[]{0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF004DFF, 0xFF750787};
			case PROGRESS -> new int[]{0xFFFFFFFF, 0xFFF5A9B8, 0xFF5BCEFA, 0xFF613915, 0xFF000000,
				0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF004DFF, 0xFF750787};
			case TRANS -> new int[]{0xFF5BCEFA, 0xFFF5A9B8, 0xFFFFFFFF, 0xFFF5A9B8, 0xFF5BCEFA};
			case BI -> new int[]{0xFFD60270, 0xFFD60270, 0xFF9B4F96, 0xFF0038A8, 0xFF0038A8};
			case PAN -> new int[]{0xFFFF218C, 0xFFFFD800, 0xFF21B1FF};
			case LESBIAN -> new int[]{0xFFD52D00, 0xFFFF9A56, 0xFFFFFFFF, 0xFFD362A4, 0xFFA30262};
			case GAY -> new int[]{0xFF078D70, 0xFF26CEAA, 0xFF98E8C1, 0xFFFFFFFF, 0xFF7BADE2, 0xFF5049CC, 0xFF3D1A78};
			case NONBINARY -> new int[]{0xFFFCF434, 0xFFFFFFFF, 0xFF9C59D1, 0xFF2C2C2C};
			case ASEXUAL -> new int[]{0xFF000000, 0xFFA3A3A3, 0xFFFFFFFF, 0xFF800080};
			case AROMANTIC -> new int[]{0xFF3DA542, 0xFFA7D379, 0xFFFFFFFF, 0xFFA9A9A9, 0xFF000000};
			case GENDERFLUID -> new int[]{0xFFFF76A4, 0xFFFFFFFF, 0xFFC011D7, 0xFF000000, 0xFF2F3CBE};
			case AGENDER -> new int[]{0xFF000000, 0xFFBCC4C7, 0xFFFFFFFF, 0xFFB7F684, 0xFFFFFFFF, 0xFFBCC4C7, 0xFF000000};
			case POLYSEXUAL -> new int[]{0xFFF61CB9, 0xFF07D569, 0xFF1C92F6};
			case DEMIGIRL -> new int[]{0xFF7F7F7F, 0xFFC4C4C4, 0xFFFDADC8, 0xFFFFFFFF, 0xFFFDADC8, 0xFFC4C4C4, 0xFF7F7F7F};
			case DEMIBOY -> new int[]{0xFF7F7F7F, 0xFFC4C4C4, 0xFF9AD9EB, 0xFFFFFFFF, 0xFF9AD9EB, 0xFFC4C4C4, 0xFF7F7F7F};
			default -> new int[]{0xFFFFFFFF};
		};
	}

	/** Farbe des Streifens an Position t (0 = oben/links, 1 = unten/rechts). */
	public static int at(int[] stripes, float t) {
		int i = Math.clamp((int) (t * stripes.length), 0, stripes.length - 1);
		return stripes[i];
	}

	/** Weicher Verlauf über alle Streifen, für dünne Leisten. */
	public static int smooth(int[] stripes, float t) {
		if (stripes.length == 1) {
			return stripes[0];
		}
		float f = Math.clamp(t, 0f, 1f) * (stripes.length - 1);
		int i = Math.min((int) f, stripes.length - 2);
		return NightTheme.lerp(stripes[i], stripes[i + 1], f - i);
	}
}
