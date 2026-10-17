package dev.lunaos.luhub.config;

import net.minecraft.network.chat.Component;

/**
 * Wie eine Statusleiste (Herzen, Hunger, Luft, XP) gezeichnet wird.
 * Die Pride-Stile malen die Füllung in den Streifen der jeweiligen Flagge.
 */
public enum BarStyle {
	VANILLA(false),
	NIGHT(false),
	CUSTOM(false),
	RAINBOW(true),
	PROGRESS(true),
	TRANS(true),
	BI(true),
	PAN(true),
	LESBIAN(true),
	GAY(true),
	NONBINARY(true),
	ASEXUAL(true),
	AROMANTIC(true),
	GENDERFLUID(true),
	AGENDER(true),
	POLYSEXUAL(true),
	DEMIGIRL(true),
	DEMIBOY(true);

	private final boolean pride;

	BarStyle(boolean pride) {
		this.pride = pride;
	}

	public boolean isPride() {
		return pride;
	}

	public Component label() {
		return Component.translatable("luhub.style." + name().toLowerCase());
	}
}
