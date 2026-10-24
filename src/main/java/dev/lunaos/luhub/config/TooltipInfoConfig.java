package dev.lunaos.luhub.config;

/** Eine Zusatz-Zeile im Item-Tooltip: an/aus und Farbe. Die Reihenfolge der Liste ist die Reihenfolge im Tooltip. */
public final class TooltipInfoConfig {
	public String id;
	public boolean enabled;
	/** ARGB; 0 = automatisch (z.B. Haltbarkeit von Grün nach Rot). */
	public int color;

	public TooltipInfoConfig() {
	}

	public TooltipInfoConfig(dev.lunaos.luhub.tooltip.TooltipInfo info) {
		this.id = info.name();
		this.enabled = info.defaultEnabled;
		this.color = info.defaultColor;
	}
}
