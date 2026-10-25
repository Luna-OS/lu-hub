package dev.lunaos.luhub.test;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/** Alter Mond und Sternenhimmel (pixelweise) – Referenz für den Pixelvergleich und die Element-Zählung. */
public final class LegacyMoon {
	private LegacyMoon() {
	}

	/** Zeichnet eine kleine Mondsichel (prozedurale Pixel-Art). */
	public static void extractMoon(GuiGraphicsExtractor graphics, int x, int y, int size, int color, boolean hovered) {
		float r = size / 2f;
		float cx = r;
		float cy = r;
		// zweiter Kreis schneidet die Sichel heraus
		float cx2 = r * 1.55f;
		float cy2 = r * 0.7f;
		float r2 = r * 0.85f;
		int shade = NightTheme.lerp(color, 0xFF8A7A3A, 0.45f);
		int glow = ARGB.color(hovered ? 90 : 45, color & 0xFFFFFF);
		for (int py = -1; py <= size; py++) {
			for (int px = -1; px <= size; px++) {
				float dx = px + 0.5f - cx;
				float dy = py + 0.5f - cy;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				float dx2 = px + 0.5f - cx2;
				float dy2 = py + 0.5f - cy2;
				float d2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
				if (d <= r && d2 > r2) {
					boolean edge = d > r - 1.1f || d2 < r2 + 1.0f;
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, edge ? shade : color);
				} else if (d <= r + 1.2f && d2 > r2 - 0.5f) {
					graphics.fill(x + px, y + py, x + px + 1, y + py + 1, glow);
				}
			}
		}
		// kleine Krater
		int crater = Math.max(1, size / 11);
		int c1x = x + Math.round(size * 0.2f);
		int c1y = y + Math.round(size * 0.45f);
		int c2x = x + Math.round(size * 0.3f);
		int c2y = y + Math.round(size * 0.72f);
		graphics.fill(c1x, c1y, c1x + crater, c1y + crater, shade);
		graphics.fill(c2x, c2y, c2x + crater, c2y + crater, shade);
	}

	/** Ersetzt den abgedunkelten Hintergrund hinter Inventaren durch einen funkelnden Nachthimmel. */
	public static void extractNightSky(GuiGraphicsExtractor graphics, int width, int height) {
		LuConfig c = LuConfig.get();
		graphics.fillGradient(0, 0, width, height, c.colorSkyTop, c.colorSkyBottom);
		if (!c.stars) {
			return;
		}
		double time = Util.getMillis() / 1000.0;
		int count = (int) (width * height / 700 * Math.max(0.2f, c.starDensity));
		for (int i = 0; i < count; i++) {
			int x = (int) (NightTheme.hash(i, 1, 77) * width);
			int y = (int) (NightTheme.hash(i, 2, 77) * height);
			float phase = NightTheme.hash(i, 3, 77) * 6.283f;
			float speed = 0.6f + NightTheme.hash(i, 4, 77) * 1.8f;
			float twinkle = (float) (0.5 + 0.5 * Math.sin(time * speed + phase));
			int alpha = (int) (60 + 195 * twinkle);
			int color = ARGB.color(alpha, c.colorStar & 0xFFFFFF);
			graphics.fill(x, y, x + 1, y + 1, color);
			if (i % 9 == 0 && twinkle > 0.75f) {
				int glow = ARGB.color(alpha / 3, c.colorStar & 0xFFFFFF);
				graphics.fill(x - 1, y, x, y + 1, glow);
				graphics.fill(x + 1, y, x + 2, y + 1, glow);
				graphics.fill(x, y - 1, x + 1, y, glow);
				graphics.fill(x, y + 1, x + 1, y + 2, glow);
			}
		}
		extractShootingStar(graphics, width, height, time, c.colorStar);
	}

	private static void extractShootingStar(GuiGraphicsExtractor graphics, int width, int height, double time, int color) {
		double period = 9.0;
		long cycle = (long) (time / period);
		double t = (time % period) / 1.2; // Sternschnuppe ist 1,2 s sichtbar
		if (t > 1.0) {
			return;
		}
		int startX = (int) (NightTheme.hash((int) cycle, 5, 91) * width * 0.7f + width * 0.2f);
		int startY = (int) (NightTheme.hash((int) cycle, 6, 91) * height * 0.35f);
		int length = 18;
		int headX = startX - (int) (t * 90);
		int headY = startY + (int) (t * 45);
		for (int i = 0; i < length; i++) {
			int alpha = (int) ((1 - i / (float) length) * 230 * (1 - t * 0.6));
			int px = headX + i * 2;
			int py = headY - i;
			graphics.fill(px, py, px + 2, py + 1, ARGB.color(alpha, color & 0xFFFFFF));
		}
	}
}
