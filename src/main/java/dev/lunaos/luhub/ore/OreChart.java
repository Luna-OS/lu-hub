package dev.lunaos.luhub.ore;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.render.BakedTextures;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Zeichnet die Erz-Verteilung über die Höhe: oben der Himmel, unten das Grundgestein, je Erz eine Spalte.
 * Die Breite der Kurve zeigt, wie viel es gibt, die Farbe ebenso (dunkel = wenig, hell leuchtend = am meisten).
 * Die perfekte Höhe ist mit einer Linie und einem ★ markiert, die eigene Höhe mit einer Linie „Du“.
 * <p>
 * Die Kurven ändern sich nie – sie werden einmal pro Größe gebacken und mit einem blit gezeichnet.
 */
public final class OreChart {
	private static final int CACHE = 24;
	private static final long[] KEYS = new long[CACHE];
	private static final Identifier[] TEXTURES = new Identifier[CACHE];
	private static int nextSlot;

	private OreChart() {
	}

	// ------------------------------------------------------------------ Umrechnung Bildschirm <-> Welt

	public static int rowOf(int worldY, OreData.Dim dim, int y, int h) {
		float t = (dim.maxY - worldY) / (float) (dim.maxY - dim.minY);
		return y + Math.round(t * (h - 1));
	}

	public static int worldYOf(int row, OreData.Dim dim, int y, int h) {
		float t = (row - y + 0.5f) / h;
		return Math.round(dim.maxY - t * (dim.maxY - dim.minY));
	}

	/** Höhenmarken für die Beschriftung. */
	public static int[] axisLabels(OreData.Dim dim) {
		return dim == OreData.Dim.OVERWORLD ? new int[]{320, 192, 64, 0, -64} : new int[]{128, 64, 0};
	}

	/** Farbe für eine relative Menge: dunkel = wenig, Erzfarbe = gut, fast weiß leuchtend = am meisten. */
	public static int heat(OreData ore, float relative) {
		int dark = NightTheme.lerp(0xFF0A0B1E, ore.color, 0.35f);
		if (relative < 0.6f) {
			return NightTheme.lerp(dark, ore.color, relative / 0.6f);
		}
		return NightTheme.lerp(ore.color, 0xFFFFFFFF, (relative - 0.6f) / 0.4f * 0.55f);
	}

	/** Die Höhe des Spielers, wenn er in dieser Dimension ist – sonst {@link Integer#MIN_VALUE}. */
	public static int playerY(OreData.Dim dim) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) {
			return Integer.MIN_VALUE;
		}
		boolean nether = mc.level.dimension() == Level.NETHER;
		boolean overworld = mc.level.dimension() == Level.OVERWORLD;
		if ((dim == OreData.Dim.NETHER && !nether) || (dim == OreData.Dim.OVERWORLD && !overworld)) {
			return Integer.MIN_VALUE;
		}
		return player.getBlockY();
	}

	// ------------------------------------------------------------------ Zeichnen

	/**
	 * @param single ein einzelnes Erz: der gute Bereich wird hinterlegt, die beste Höhe geht über die ganze Breite
	 */
	public static void extract(GuiGraphicsExtractor graphics, List<OreData> ores, OreData.Dim dim, int x, int y, int w, int h,
		int mouseX, int mouseY, boolean single) {
		LuConfig c = LuConfig.get();
		Font font = Minecraft.getInstance().font;
		// Himmel -> Stein -> Tiefenschiefer (bzw. Nether-Rot)
		int stone = dim == OreData.Dim.OVERWORLD ? 0xFF2A2D44 : 0xFF3A1C2A;
		graphics.fillGradient(x, y, x + w, y + h, NightTheme.lerp(c.colorSkyTop, 0xFF000000, 0.2f), stone);
		graphics.outline(x - 1, y - 1, w + 2, h + 2, c.colorBorder);
		if (dim == OreData.Dim.OVERWORLD) {
			int zero = rowOf(0, dim, y, h);
			graphics.fill(x, zero, x + w, y + h, 0x40000000); // Tiefenschiefer
			int sea = rowOf(63, dim, y, h);
			graphics.fill(x, sea, x + w, sea + 1, 0x904A8CFF); // Meeresspiegel
			graphics.fill(x, zero, x + w, zero + 1, 0x60FFFFFF);
		} else {
			int lava = rowOf(31, dim, y, h);
			graphics.fill(x, lava, x + w, y + h, 0x30FF6A1A); // Höhe der Lavaseen
		}
		graphics.fill(x, y + h - 2, x + w, y + h, 0xFF101018); // Grundgestein

		graphics.blit(RenderPipelines.GUI_TEXTURED, curves(ores, dim, w, h, single), x, y, 0, 0, w, h, w, h);

		if (single) {
			// „★ Perfekt“ direkt an der besten Höhe (unten im Bild darunter, sonst darüber)
			OreData ore = ores.getFirst();
			int best = rowOf(ore.bestY, dim, y, h);
			String perfect = "★ " + Component.translatable("luhub.ores.perfect").getString();
			int textW = Math.round(font.width(perfect) * 0.5f);
			int textY = best - y < 10 ? best + 3 : best - 6;
			if (best > y + h - 12) {
				textY = best - 6;
			}
			smallText(graphics, font, perfect, x + w - textW - 5, textY, 0.5f, ore.color);
		}

		// eigene Höhe
		int playerY = playerY(dim);
		if (playerY != Integer.MIN_VALUE && playerY >= dim.minY && playerY <= dim.maxY) {
			int row = rowOf(playerY, dim, y, h);
			graphics.fill(x, row, x + w, row + 1, 0xC0FFFFFF);
			graphics.fill(x, row - 1, x + 3, row + 2, 0xFFFFFFFF);
			smallText(graphics, font, Component.translatable("luhub.ores.you").getString(), x + 4, row - 6, 0.5f, 0xFFFFFFFF);
		}
		// Maus-Linie
		if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
			graphics.fill(x, mouseY, x + w, mouseY + 1, 0x50FFFFFF);
		}
	}

	/**
	 * Text zu einem Erz: perfekte Höhe (groß), guter Bereich, Bewertung der eigenen Höhe und ein Tipp.
	 * @param bottom unterste Zeile, bis zu der der Tipp umbrochen wird
	 */
	public static void extractInfo(GuiGraphicsExtractor graphics, OreData ore, int x, int y, int width, int bottom) {
		LuConfig c = LuConfig.get();
		Font font = Minecraft.getInstance().font;
		int dim = NightTheme.lerp(c.colorLabel, 0xFF000000, 0.3f);
		graphics.text(font, Component.translatable("luhub.ores.best"), x, y, dim, false);
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y + 10);
		pose.scale(1.5f, 1.5f);
		graphics.text(font, "Y = " + ore.bestY, 0, 0, ore.color, true);
		pose.popMatrix();
		graphics.text(font, Component.translatable("luhub.ores.range", ore.rangeMin, ore.rangeMax), x, y + 24, c.colorLabel, false);
		graphics.text(font, Component.translatable("luhub.ores.presence", ore.presenceMin(), ore.presenceMax()), x, y + 35, dim, false);
		int lineY = y + 46;
		int playerY = playerY(ore.dim);
		if (playerY != Integer.MIN_VALUE) {
			float rel = ore.relative(playerY);
			Component here = Component.translatable("luhub.ores.here", playerY).append(OreData.rating(rel));
			float fit = Math.min(1f, width / (float) Math.max(1, font.width(here)));
			pose.pushMatrix();
			pose.translate(x, lineY);
			pose.scale(fit, fit);
			graphics.text(font, here, 0, 0, OreData.ratingColor(rel), false);
			pose.popMatrix();
			lineY += 11;
		}
		// Tipp etwas kleiner, damit er ganz hineinpasst
		float scale = 0.85f;
		List<net.minecraft.util.FormattedCharSequence> tip = font.split(ore.tip(), Math.round(width / scale));
		int maxLines = Math.max(0, (int) ((bottom - lineY) / (9 * scale)));
		pose.pushMatrix();
		pose.translate(x, lineY);
		pose.scale(scale, scale);
		for (int i = 0; i < Math.min(tip.size(), maxLines); i++) {
			graphics.text(font, tip.get(i), 0, i * 9, 0xFFC9CDEE, false);
		}
		if (tip.size() > maxLines && maxLines > 0) {
			graphics.text(font, "…", Math.round(width / scale) - 6, (maxLines - 1) * 9, 0xFFC9CDEE, false);
		}
		pose.popMatrix();
	}

	/** Höhen-Beschriftung links neben dem Diagramm (rechtsbündig bis x). */
	public static void extractAxis(GuiGraphicsExtractor graphics, OreData.Dim dim, int right, int y, int h, float scale) {
		Font font = Minecraft.getInstance().font;
		for (int label : axisLabels(dim)) {
			int ly = rowOf(label, dim, y, h);
			String text = String.valueOf(label);
			int width = Math.round(font.width(text) * scale);
			smallText(graphics, font, text, right - width, ly - Math.round(4 * scale), scale, 0xFF9AA4D8);
		}
	}

	/** Farb-Legende „wenig → viel“ (Breite w, Höhe 4 + Text darunter). */
	public static void extractLegend(GuiGraphicsExtractor graphics, OreData ore, int x, int y, int w) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, legend(ore, w), x, y, 0, 0, w, 4, w, 4);
		graphics.outline(x - 1, y - 1, w + 2, 6, LuConfig.get().colorBorder);
		Font font = Minecraft.getInstance().font;
		smallText(graphics, font, Component.translatable("luhub.ores.little").getString(), x, y + 6, 0.5f, 0xFF9AA4D8);
		String much = Component.translatable("luhub.ores.much").getString();
		smallText(graphics, font, much, x + w - Math.round(font.width(much) * 0.5f), y + 6, 0.5f, 0xFFE8ECFF);
	}

	private static void smallText(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, float scale, int color) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		graphics.text(font, text, 0, 0, color, true);
		pose.popMatrix();
	}

	/** Tooltip für eine Mausposition im Diagramm – oder null außerhalb. */
	public static List<Component> tooltip(int mouseX, int mouseY, List<OreData> ores, OreData.Dim dim, int x, int y, int w, int h) {
		if (mouseX < x || mouseX >= x + w || mouseY < y || mouseY >= y + h) {
			return null;
		}
		int wy = worldYOf(mouseY, dim, y, h);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("Y = " + wy));
		List<OreData> shown = ores;
		if (ores.size() > 1) {
			int col = w / ores.size();
			shown = List.of(ores.get(Math.min(ores.size() - 1, (mouseX - x) / col)));
		}
		for (OreData ore : shown) {
			float rel = ore.relative(wy);
			lines.add(Component.empty().append(ore.label()).append(": ").append(OreData.rating(rel))
				.append(String.format(Locale.ROOT, " (%d %%)", Math.round(rel * 100))).withColor(OreData.ratingColor(rel) & 0xFFFFFF));
			lines.add(Component.translatable("luhub.ores.best_short", ore.bestY).withColor(0xC9CDEE));
			if (ore.density(wy, true) > 0f) {
				lines.add(Component.translatable("luhub.ores." + ore.name().toLowerCase(Locale.ROOT) + ".special").withColor(0x9AA4D8));
			}
		}
		return lines;
	}

	// ------------------------------------------------------------------ gebackene Kurven

	private static Identifier cached(long key, java.util.function.Supplier<Identifier> bake) {
		for (int i = 0; i < CACHE; i++) {
			if (TEXTURES[i] != null && KEYS[i] == key) {
				return TEXTURES[i];
			}
		}
		int slot = nextSlot;
		nextSlot = (nextSlot + 1) % CACHE;
		BakedTextures.release(TEXTURES[slot]);
		TEXTURES[slot] = bake.get();
		KEYS[slot] = key;
		return TEXTURES[slot];
	}

	private static Identifier curves(List<OreData> ores, OreData.Dim dim, int w, int h, boolean single) {
		long oreKey = single ? ores.getFirst().ordinal() + 1 : 0;
		long key = 1L << 62 | oreKey << 40 | (long) dim.ordinal() << 36 | (long) (w & 0xFFFF) << 16 | (h & 0xFFFF);
		return cached(key, () -> bakeCurves(ores, dim, w, h, single));
	}

	private static Identifier legend(OreData ore, int w) {
		long key = 2L << 62 | (long) ore.ordinal() << 40 | (w & 0xFFFF);
		return cached(key, () -> BakedTextures.bake("ore_legend", w, 4, (px, py) -> heat(ore, px / (float) Math.max(1, w - 1))));
	}

	private static Identifier bakeCurves(List<OreData> ores, OreData.Dim dim, int w, int h, boolean single) {
		int[] pixels = new int[w * h];
		int col = w / ores.size();
		for (int i = 0; i < ores.size(); i++) {
			OreData ore = ores.get(i);
			int left = single ? 0 : i * col;
			int right = single ? w : (i + 1) * col;
			int cx = left + (right - left) / 2;
			int maxHalf = (right - left) / 2 - 2;
			float max = ore.maxDensity();
			if (single) {
				// guter Bereich hinterlegt
				int r0 = rowOf(ore.rangeMax, dim, 0, h);
				int r1 = rowOf(ore.rangeMin, dim, 0, h);
				fill(pixels, w, 0, r0, w, r1 + 1, ARGB.color(0x30, ore.color & 0xFFFFFF));
			}
			for (int row = 0; row < h - 2; row++) {
				int wy = worldYOf(row, dim, 0, h);
				float normal = ore.density(wy, false);
				float special = ore.density(wy, true);
				if (special > 0f && (row & 1) == 0) {
					// nur in bestimmten Biomen/Höhlen: schraffiert
					int half = Math.round((float) Math.sqrt((normal + special) / max) * maxHalf);
					if (half > 0) {
						fill(pixels, w, cx - half, row, cx + half, row + 1, ARGB.color(0x80, heat(ore, (normal + special) / max) & 0xFFFFFF));
					}
				}
				if (normal > 0f) {
					float rel = normal / max;
					int half = Math.max(1, Math.round((float) Math.sqrt(rel) * maxHalf));
					fill(pixels, w, cx - half, row, cx + half, row + 1, heat(ore, rel));
				}
			}
			// perfekte Höhe: helle Linie und Marke in Erzfarbe
			int best = rowOf(ore.bestY, dim, 0, h);
			int lineLeft = single ? 0 : left + 1;
			int lineRight = single ? w : right - 1;
			fill(pixels, w, lineLeft, best, lineRight, best + 1, 0xE0FFFFFF);
			fill(pixels, w, lineRight - 3, best - 2, lineRight, best + 3, ore.color);
		}
		return BakedTextures.bake("ore_chart", w, h, (px, py) -> pixels[py * w + px]);
	}

	/** Rechteck mit Alpha-Mischung in einen Pixelpuffer zeichnen. */
	private static void fill(int[] pixels, int w, int x0, int y0, int x1, int y1, int color) {
		int h = pixels.length / w;
		for (int y = Math.max(0, y0); y < Math.min(h, y1); y++) {
			for (int x = Math.max(0, x0); x < Math.min(w, x1); x++) {
				pixels[y * w + x] = blend(pixels[y * w + x], color);
			}
		}
	}

	private static int blend(int dst, int src) {
		int sa = src >>> 24;
		if (sa == 255 || dst == 0) {
			return src;
		}
		if (sa == 0) {
			return dst;
		}
		int da = dst >>> 24;
		float s = sa / 255f;
		float d = da / 255f * (1 - s);
		float a = s + d;
		int r = Math.round((((src >> 16) & 0xFF) * s + ((dst >> 16) & 0xFF) * d) / a);
		int g = Math.round((((src >> 8) & 0xFF) * s + ((dst >> 8) & 0xFF) * d) / a);
		int b = Math.round(((src & 0xFF) * s + (dst & 0xFF) * d) / a);
		return Math.round(a * 255) << 24 | r << 16 | g << 8 | b;
	}
}
