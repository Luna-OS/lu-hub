package dev.lunaos.luhub.config;

/** Maße der Rüstungs-HUD – abhängig von Ausrichtung, Anzahl der Slots und Haltbarkeitsanzeige. */
public final class ArmorHudLayout {
	public static final int SLOT = 18;
	public static final int GAP = 2;
	public static final int BAR = 3;
	public static final int TEXT = 6;

	private ArmorHudLayout() {
	}

	public static int slotCount() {
		return LuConfig.get().armorShowHands ? 6 : 4;
	}

	public static int cellHeight() {
		LuConfig c = LuConfig.get();
		return SLOT + (c.armorDurability.bar() ? BAR : 0) + (c.armorDurability.text() ? TEXT : 0);
	}

	public static boolean horizontal() {
		return LuConfig.get().armorOrientation == ArmorHudOrientation.HORIZONTAL;
	}

	public static int width() {
		return horizontal() ? slotCount() * (SLOT + GAP) - GAP : SLOT;
	}

	public static int height() {
		return horizontal() ? cellHeight() : slotCount() * (cellHeight() + GAP) - GAP;
	}
}
