package dev.lunaos.luhub.config;

import net.minecraft.network.chat.Component;

public enum ArmorHudOrientation {
	HORIZONTAL,
	VERTICAL;

	public Component label() {
		return Component.translatable("luhub.armor.orientation." + name().toLowerCase());
	}
}
