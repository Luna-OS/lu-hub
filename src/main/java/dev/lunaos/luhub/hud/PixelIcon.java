package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.render.BakedTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Kleine Pixel-Art-Symbole (9x9). So können Herzen, Hunger und Luftblasen in jeder Farbe und jeder Pride-Flagge
 * erscheinen, ohne dass für jede Variante eine eigene Textur-Datei nötig ist.
 * <p>
 * Jede Variante (Stil, Farben, voll/halb/leer, Blinken ...) wird einmal in eine Textur gebacken
 * ({@link #bake}) und danach mit einem einzigen blit gezeichnet ({@link #blit}).
 * <p>
 * Zeichen in der Maske: '.' leer, 'o' Umriss, '#' Füllung, 'h' Glanzlicht, 'w' Knochen/Weiß.
 */
public final class PixelIcon {
	public static final PixelIcon HEART = new PixelIcon("heart",
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

	public static final PixelIcon FOOD = new PixelIcon("food",
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

	public static final PixelIcon BUBBLE = new PixelIcon("bubble",
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

	public static final PixelIcon ARMOR = new PixelIcon("armor",
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

	public final String name;
	public final int width;
	public final int height;
	private final char[][] mask;
	/** erste und letzte Zeile, die Füllung enthält – für die Flaggenstreifen. */
	public final int fillTop;
	public final int fillBottom;
	/** Varianten mit festen Farben (Tooltip, Vorschau ...), nach Schlüssel. */
	private final Map<String, Identifier> fixed = new HashMap<>();

	private PixelIcon(String name, String... rows) {
		this.name = name;
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

	/** Backt eine Variante in eine neue Textur. Freigeben mit {@link BakedTextures#release}. */
	public Identifier bake(Painter painter) {
		return BakedTextures.bake(name, width, height, (px, py) -> painter.color(mask[py][px], px, py));
	}

	/**
	 * Variante mit festen Farben: wird beim ersten Aufruf gebacken und bleibt danach im Speicher.
	 * Der Schlüssel muss die Variante eindeutig beschreiben (am besten eine String-Konstante).
	 */
	public Identifier fixed(String key, Painter painter) {
		Identifier id = fixed.get(key);
		if (id == null) {
			id = bake(painter);
			fixed.put(key, id);
		}
		return id;
	}

	/** Zeichnet eine gebackene Variante – ein einziges GUI-Element. */
	public void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, width, height, width, height);
	}

	/** Wie {@link #blit}, aber mit Farbe/Deckkraft multipliziert (z.B. für pulsierende Vorschauen). */
	public void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int tint) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, width, height, width, height, tint);
	}
}
