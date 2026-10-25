package dev.lunaos.luhub.test;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Kleine Pixel-Art-Symbole (9x9), die direkt mit Rechtecken gezeichnet werden.
 * So können Herzen, Hunger und Luftblasen in jeder Farbe und jeder Pride-Flagge erscheinen,
 * ohne dass für jede Variante eine eigene Textur nötig ist.
 * <p>
 * Zeichen in der Maske: '.' leer, 'o' Umriss, '#' Füllung, 'h' Glanzlicht, 'w' Knochen/Weiß.
 */
/** Alte Version (vor dem Textur-Cache) – Referenz für den Pixelvergleich. */
public final class LegacyPixelIcon {
	public static final LegacyPixelIcon HEART = new LegacyPixelIcon(
		".oo...oo.",
		"ohho.o##o",
		"oh##o###o",
		"o#######o",
		"o#######o",
		".o#####o.",
		"..o###o..",
		"...o#o...",
		"....o...."
	);

	public static final LegacyPixelIcon FOOD = new LegacyPixelIcon(
		"....ooo..",
		"...o#h#o.",
		"..o#h###o",
		"..o#####o",
		"..o####o.",
		".oo###o..",
		"owwooo...",
		"owo......",
		".o......."
	);

	public static final LegacyPixelIcon BUBBLE = new LegacyPixelIcon(
		"..ooooo..",
		".o#####o.",
		"o#hh####o",
		"o#h#####o",
		"o#######o",
		"o#######o",
		"o#######o",
		".o#####o.",
		"..ooooo.."
	);

	public static final LegacyPixelIcon ARMOR = new LegacyPixelIcon(
		".oo...oo.",
		"ohoooooho",
		"o#h###h#o",
		"o#######o",
		".o#####o.",
		".o#####o.",
		".o#####o.",
		".o##h##o.",
		"..ooooo.."
	);

	public final int width;
	public final int height;
	private final char[][] mask;
	/** erste und letzte Zeile, die Füllung enthält – für die Flaggenstreifen. */
	public final int fillTop;
	public final int fillBottom;

	private LegacyPixelIcon(String... rows) {
		this.height = rows.length;
		this.width = rows[0].length();
		this.mask = new char[height][];
		int top = height;
		int bottom = 0;
		for (int y = 0; y < height; y++) {
			mask[y] = rows[y].toCharArray();
			for (char ch : mask[y]) {
				if (ch == '#' || ch == 'h') {
					top = Math.min(top, y);
					bottom = Math.max(bottom, y);
				}
			}
		}
		this.fillTop = top;
		this.fillBottom = bottom;
	}

	/** Liefert für jedes Maskenpixel eine ARGB-Farbe (0 = nicht zeichnen). */
	@FunctionalInterface
	public interface Painter {
		int color(char type, int px, int py);
	}

	/** Relative Position einer Zeile innerhalb der gefüllten Fläche (0 = oben, 1 = unten). */
	public float rowT(int py) {
		if (fillBottom <= fillTop) {
			return 0f;
		}
		return (py - fillTop) / (float) (fillBottom - fillTop + 1) + 0.5f / (fillBottom - fillTop + 1);
	}

	public void draw(GuiGraphicsExtractor graphics, int x, int y, Painter painter) {
		for (int py = 0; py < height; py++) {
			int runStart = -1;
			int runColor = 0;
			for (int px = 0; px <= width; px++) {
				int color = px < width ? painter.color(mask[py][px], px, py) : 0;
				if (runStart >= 0 && color != runColor) {
					graphics.fill(x + runStart, y + py, x + px, y + py + 1, runColor);
					runStart = -1;
				}
				if (runStart < 0 && color != 0) {
					runStart = px;
					runColor = color;
				}
			}
		}
	}
}
