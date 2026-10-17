package dev.lunaos.luhub.config;

import net.minecraft.network.chat.Component;

/** Wie die Haltbarkeit in der Rüstungs-HUD angezeigt wird. */
public enum ArmorDurability {
	BAR,
	PERCENT,
	NUMBER,
	BAR_AND_PERCENT;

	public boolean bar() {
		return this == BAR || this == BAR_AND_PERCENT;
	}

	public boolean text() {
		return this != BAR;
	}

	public Component label() {
		return Component.translatable("luhub.armor.durability." + name().toLowerCase());
	}
}
