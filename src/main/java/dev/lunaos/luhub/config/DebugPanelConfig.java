package dev.lunaos.luhub.config;

/** Einstellungen für ein F3-Panel (ein Panel = ein Vanilla-Debug-Eintrag, z.B. "Position"). */
public final class DebugPanelConfig {
	public boolean hidden = false;
	/** false = automatisch in der linken/rechten Spalte stapeln, true = frei platziert. */
	public boolean placed = false;
	public Anchor anchor = Anchor.TOP_LEFT;
	/** Abstand des Ankerpunkts des Panels zum Ankerpunkt des Bildschirms. */
	public int x = 0;
	public int y = 0;
	public float scale = 1f;
	/** Akzentfarbe; 0 = automatische Farbe für diesen Eintrag. */
	public int color = 0;
}
