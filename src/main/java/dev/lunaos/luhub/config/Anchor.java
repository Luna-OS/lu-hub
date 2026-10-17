package dev.lunaos.luhub.config;

/**
 * Bildschirm-Ecke/-Kante, an der ein F3-Panel "klebt". Die Position wird relativ
 * zu diesem Anker gespeichert, damit das Layout bei jeder Fenstergröße passt.
 */
public enum Anchor {
	TOP_LEFT(0, 0),
	TOP_CENTER(1, 0),
	TOP_RIGHT(2, 0),
	CENTER_LEFT(0, 1),
	CENTER(1, 1),
	CENTER_RIGHT(2, 1),
	BOTTOM_LEFT(0, 2),
	BOTTOM_CENTER(1, 2),
	BOTTOM_RIGHT(2, 2);

	public final int col;
	public final int row;

	Anchor(int col, int row) {
		this.col = col;
		this.row = row;
	}

	/** X-Koordinate des Ankerpunkts auf dem Bildschirm. */
	public int baseX(int screenWidth) {
		return col * screenWidth / 2;
	}

	public int baseY(int screenHeight) {
		return row * screenHeight / 2;
	}

	/** Anteil der Panel-Breite, der links vom Ankerpunkt liegt (0, 0.5 oder 1). */
	public float pivotX() {
		return col / 2f;
	}

	public float pivotY() {
		return row / 2f;
	}

	/** Wählt den Anker nach dem Bildschirm-Drittel, in dem die Mitte des Panels liegt. */
	public static Anchor nearest(float centerX, float centerY, int screenWidth, int screenHeight) {
		int c = Math.clamp((int) (centerX / (screenWidth / 3f)), 0, 2);
		int r = Math.clamp((int) (centerY / (screenHeight / 3f)), 0, 2);
		for (Anchor a : values()) {
			if (a.col == c && a.row == r) {
				return a;
			}
		}
		return TOP_LEFT;
	}
}
