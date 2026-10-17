package dev.lunaos.luhub.config;

import net.minecraft.network.chat.Component;

/**
 * Die HUD-Elemente, die Lu Hub verschieben, skalieren und umgestalten kann.
 * Die "Box" ist die Vanilla-Position, an der das Element normalerweise sitzt.
 */
public enum HudElementType {
	HOTBAR(182, 22, false, 0xFF969FEB),
	HEALTH(81, 9, true, 0xFFF06BA8),
	ARMOR(81, 9, false, 0xFFB9C0FF),
	FOOD(81, 9, true, 0xFFFFC46B),
	AIR(81, 9, true, 0xFF8FE3FF),
	XP_BAR(182, 5, true, 0xFF9DF5C4),
	XP_LEVEL(30, 9, true, 0xFF9DF5C4),
	/** Eigene Rüstungs-HUD: Rüstungsteile (und Hände) mit Haltbarkeit. Stil = Farbe der Haltbarkeitsleisten. */
	ARMOR_HUD(0, 0, true, 0xFF9DF5C4);

	private final int width;
	private final int height;
	public final boolean styleable;
	public final int defaultFill;

	HudElementType(int width, int height, boolean styleable, int defaultFill) {
		this.width = width;
		this.height = height;
		this.styleable = styleable;
		this.defaultFill = defaultFill;
	}

	/** false, wenn die Funktion abgeschaltet ist, weil die Original-Mod installiert ist. */
	public boolean available() {
		return this != ARMOR_HUD || !dev.lunaos.luhub.compat.ExternalMods.UKUS_ARMOR_HUD;
	}

	public int width() {
		return this == ARMOR_HUD ? ArmorHudLayout.width() : width;
	}

	public int height() {
		return this == ARMOR_HUD ? ArmorHudLayout.height() : height;
	}

	public int defaultX(int screenWidth) {
		return switch (this) {
			case HOTBAR, HEALTH, ARMOR, XP_BAR -> screenWidth / 2 - 91;
			case FOOD, AIR -> screenWidth / 2 + 91 - 81;
			case XP_LEVEL -> screenWidth / 2 - 15;
			// links neben der Hotbar (mit Platz für den Zweithand-Slot); passt das nicht, ganz an den linken Rand
			case ARMOR_HUD -> Math.max(2, screenWidth / 2 - 91 - 29 - 6 - width());
		};
	}

	public int defaultY(int screenHeight) {
		return switch (this) {
			case HOTBAR -> screenHeight - 22;
			case HEALTH, FOOD -> screenHeight - 39;
			case ARMOR, AIR -> screenHeight - 49;
			case XP_BAR -> screenHeight - 29;
			case XP_LEVEL -> screenHeight - 35;
			case ARMOR_HUD -> armorFitsBesideHotbar() ? screenHeight - height() - 1 : screenHeight - 24 - height() - 2;
		};
	}

	private static boolean armorFitsBesideHotbar() {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		return screenWidth / 2 - 91 - 29 - 6 - ArmorHudLayout.width() >= 2;
	}

	public BarStyle defaultStyle() {
		return styleable ? BarStyle.NIGHT : BarStyle.VANILLA;
	}

	public static HudElementConfig createDefault(HudElementType type) {
		return new HudElementConfig(type.defaultStyle(), type.defaultFill);
	}

	public Component label() {
		return Component.translatable("luhub.hud." + name().toLowerCase());
	}
}
