package dev.lunaos.luhub.config;

/** Einstellungen für ein einzelnes HUD-Element (Position, Größe, Aussehen). */
public final class HudElementConfig {
	public boolean visible = true;
	/** Verschiebung gegenüber der Vanilla-Position, in GUI-Pixeln. */
	public int dx = 0;
	public int dy = 0;
	public float scale = 1f;
	public BarStyle style = BarStyle.NIGHT;
	/** Farben für den Stil "Eigene Farben". */
	public int fillColor = 0xFFE86AA8;
	public int emptyColor = 0xFF1A1D3A;
	public int outlineColor = 0xFF07081A;
	/** Kleine Sternchen / Glitzer auf den Symbolen. */
	public boolean sparkle = true;

	public HudElementConfig() {
	}

	public HudElementConfig(BarStyle style, int fillColor) {
		this.style = style;
		this.fillColor = fillColor;
	}
}
