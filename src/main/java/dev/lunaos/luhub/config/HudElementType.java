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
	XP_LEVEL(30, 9, true, 0xFF9DF5C4);

	public final int width;
	public final int height;
	public final boolean styleable;
	public final int defaultFill;

	HudElementType(int width, int height, boolean styleable, int defaultFill) {
		this.width = width;
		this.height = height;
		this.styleable = styleable;
		this.defaultFill = defaultFill;
	}

	public int defaultX(int screenWidth) {
		return switch (this) {
			case HOTBAR, HEALTH, ARMOR, XP_BAR -> screenWidth / 2 - 91;
			case FOOD, AIR -> screenWidth / 2 + 91 - 81;
			case XP_LEVEL -> screenWidth / 2 - 15;
		};
	}

	public int defaultY(int screenHeight) {
		return switch (this) {
			case HOTBAR -> screenHeight - 22;
			case HEALTH, FOOD -> screenHeight - 39;
			case ARMOR, AIR -> screenHeight - 49;
			case XP_BAR -> screenHeight - 29;
			case XP_LEVEL -> screenHeight - 35;
		};
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
