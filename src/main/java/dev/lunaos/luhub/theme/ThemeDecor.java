package dev.lunaos.luhub.theme;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.render.BakedTextures;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/**
 * Zusätzliche Deko: Sternenhimmel-Hintergrund, Sternschnuppe und Mond.
 * <p>
 * Sterne, Sternschnuppe und Mond werden einmal in Texturen gebacken (neu nur bei anderer Bildschirmgröße,
 * Sterndichte oder Farbe) und mit wenigen blit-Aufrufen gezeichnet. Die Sterne sind in {@link #GROUPS} Gruppen
 * aufgeteilt, die jeweils gemeinsam funkeln – so bleibt der Himmel lebendig, kostet aber nur eine Handvoll
 * GUI-Elemente statt einem pro Stern.
 */
public final class ThemeDecor {
	public static final int MOON_SIZE = 11;
	/** Anzahl der Funkel-Gruppen (je eine Textur für Sterne und eine für deren Schein). */
	private static final int GROUPS = 4;
	private static final int SHOOTING_STAR_LENGTH = 18;

	/** Hintergrund hinter Inventaren und Menüs: Sterne überall, einige mit kleinem Schein. */
	private static final StarField MENU_STARS = new StarField("menu_stars", 77, 700, true, false);
	/** Titelbildschirm: Sterne vor allem im oberen Himmel, ohne Schein. */
	private static final StarField TITLE_STARS = new StarField("title_stars", 13, 900, false, true);

	private static Identifier shootingStar;
	private static int shootingStarColor;

	private static final int MOON_CACHE = 8;
	private static final Identifier[] MOON_IDS = new Identifier[MOON_CACHE];
	private static final int[] MOON_SIZES = new int[MOON_CACHE];
	private static final int[] MOON_COLORS = new int[MOON_CACHE];
	private static final boolean[] MOON_HOVERED = new boolean[MOON_CACHE];
	private static int nextMoonSlot;

	private ThemeDecor() {
	}

	/** Ersetzt den abgedunkelten Hintergrund hinter Inventaren durch einen funkelnden Nachthimmel. */
	public static void extractNightSky(GuiGraphicsExtractor graphics, int width, int height) {
		LuConfig c = LuConfig.get();
		graphics.fillGradient(0, 0, width, height, c.colorSkyTop, c.colorSkyBottom);
		if (!c.stars) {
			return;
		}
		double time = Util.getMillis() / 1000.0;
		MENU_STARS.extract(graphics, width, height, c.starDensity, c.colorStar, time);
		extractShootingStar(graphics, width, height, time, c.colorStar);
	}

	/** Eine Sternschnuppe alle 9 Sekunden, 1,2 Sekunden lang sichtbar – eine gebackene Textur. */
	private static void extractShootingStar(GuiGraphicsExtractor graphics, int width, int height, double time, int color) {
		double period = 9.0;
		long cycle = (long) (time / period);
		double t = (time % period) / 1.2; // Sternschnuppe ist 1,2 s sichtbar
		if (t > 1.0) {
			return;
		}
		if (shootingStar == null || shootingStarColor != color) {
			BakedTextures.release(shootingStar);
			shootingStarColor = color;
			int length = SHOOTING_STAR_LENGTH;
			// Segment i: 2×1 Pixel bei (2i, length-1-i), nach hinten immer durchsichtiger
			shootingStar = BakedTextures.bake("shooting_star", length * 2, length, (px, py) -> {
				int i = px / 2;
				if (py != length - 1 - i) {
					return 0;
				}
				int alpha = (int) ((1 - i / (float) length) * 230);
				return alpha <= 0 ? 0 : ARGB.color(alpha, color & 0xFFFFFF);
			});
		}
		int startX = (int) (NightTheme.hash((int) cycle, 5, 91) * width * 0.7f + width * 0.2f);
		int startY = (int) (NightTheme.hash((int) cycle, 6, 91) * height * 0.35f);
		int headX = startX - (int) (t * 90);
		int headY = startY + (int) (t * 45);
		int fade = (int) ((1 - t * 0.6) * 255);
		int length = SHOOTING_STAR_LENGTH;
		graphics.blit(RenderPipelines.GUI_TEXTURED, shootingStar, headX, headY - (length - 1), 0, 0,
			length * 2, length, length * 2, length, ARGB.color(fade, 0xFFFFFF));
	}

	/** Titelbildschirm: halbtransparenter Nachthimmel über dem Panorama, Sterne und ein großer Mond. */
	public static void extractTitleSky(GuiGraphicsExtractor graphics, int width, int height) {
		LuConfig c = LuConfig.get();
		if (!c.themeEnabled || !c.titleScreenTheme) {
			return;
		}
		int top = ARGB.color(170, c.colorSkyTop & 0xFFFFFF);
		int bottom = ARGB.color(90, c.colorSkyBottom & 0xFFFFFF);
		graphics.fillGradient(0, 0, width, height, top, bottom);
		if (c.stars) {
			double time = Util.getMillis() / 1000.0;
			TITLE_STARS.extract(graphics, width, height, c.starDensity, c.colorStar, time);
			extractShootingStar(graphics, width, height, time, c.colorStar);
		}
		int moonSize = Math.max(16, height / 9);
		extractMoon(graphics, width - moonSize - Math.max(12, width / 30), Math.max(10, height / 25), moonSize, c.colorMoon, false);
	}

	/** Position des Mondes oben rechts im Container-Fenster. */
	public static int moonX(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		return acc.luhub$getLeftPos() + acc.luhub$getImageWidth() - MOON_SIZE - 5;
	}

	public static int moonY(AbstractContainerScreen<?> screen) {
		return ((AbstractContainerScreenAccessor) screen).luhub$getTopPos() + 4;
	}

	public static boolean isOverMoon(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		int x = moonX(screen);
		int y = moonY(screen);
		return mouseX >= x && mouseX < x + MOON_SIZE && mouseY >= y && mouseY < y + MOON_SIZE;
	}

	/** Zeichnet eine kleine Mondsichel (prozedurale Pixel-Art) – gebacken, ein blit. */
	public static void extractMoon(GuiGraphicsExtractor graphics, int x, int y, int size, int color, boolean hovered) {
		Identifier moon = moonTexture(size, color, hovered);
		int texSize = size + 2; // inkl. einem Pixel Schein ringsum
		graphics.blit(RenderPipelines.GUI_TEXTURED, moon, x - 1, y - 1, 0, 0, texSize, texSize, texSize, texSize);
	}

	private static Identifier moonTexture(int size, int color, boolean hovered) {
		for (int i = 0; i < MOON_CACHE; i++) {
			if (MOON_IDS[i] != null && MOON_SIZES[i] == size && MOON_COLORS[i] == color && MOON_HOVERED[i] == hovered) {
				return MOON_IDS[i];
			}
		}
		int slot = nextMoonSlot;
		nextMoonSlot = (nextMoonSlot + 1) % MOON_CACHE;
		BakedTextures.release(MOON_IDS[slot]);
		MOON_IDS[slot] = bakeMoon(size, color, hovered);
		MOON_SIZES[slot] = size;
		MOON_COLORS[slot] = color;
		MOON_HOVERED[slot] = hovered;
		return MOON_IDS[slot];
	}

	private static Identifier bakeMoon(int size, int color, boolean hovered) {
		float r = size / 2f;
		float cx = r;
		float cy = r;
		// zweiter Kreis schneidet die Sichel heraus
		float cx2 = r * 1.55f;
		float cy2 = r * 0.7f;
		float r2 = r * 0.85f;
		int shade = NightTheme.lerp(color, 0xFF8A7A3A, 0.45f);
		int glow = ARGB.color(hovered ? 90 : 45, color & 0xFFFFFF);
		// kleine Krater
		int crater = Math.max(1, size / 11);
		int c1x = Math.round(size * 0.2f);
		int c1y = Math.round(size * 0.45f);
		int c2x = Math.round(size * 0.3f);
		int c2y = Math.round(size * 0.72f);
		// Texturkoordinate (tx, ty) entspricht dem Mond-Pixel (tx - 1, ty - 1)
		return BakedTextures.bake("moon", size + 2, size + 2, (tx, ty) -> {
			int px = tx - 1;
			int py = ty - 1;
			if ((px >= c1x && px < c1x + crater && py >= c1y && py < c1y + crater)
				|| (px >= c2x && px < c2x + crater && py >= c2y && py < c2y + crater)) {
				return shade;
			}
			float dx = px + 0.5f - cx;
			float dy = py + 0.5f - cy;
			float d = (float) Math.sqrt(dx * dx + dy * dy);
			float dx2 = px + 0.5f - cx2;
			float dy2 = py + 0.5f - cy2;
			float d2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);
			if (d <= r && d2 > r2) {
				boolean edge = d > r - 1.1f || d2 < r2 + 1.0f;
				return edge ? shade : color;
			}
			if (d <= r + 1.2f && d2 > r2 - 0.5f) {
				return glow;
			}
			return 0;
		});
	}

	/**
	 * Ein Sternenfeld in Bildschirmgröße. Jede Funkel-Gruppe ist eine Textur (Sterne deckend in Sternfarbe),
	 * deren Deckkraft pro Frame per Tönung animiert wird; der Schein einzelner Sterne liegt in einer zweiten
	 * Textur je Gruppe und erscheint nur, wenn die Gruppe gerade hell leuchtet.
	 */
	private static final class StarField {
		private final String name;
		private final int seed;
		private final int areaPerStar;
		private final boolean withGlow;
		private final boolean upperSky;
		private final Identifier[] stars = new Identifier[GROUPS];
		private final Identifier[] glows = new Identifier[GROUPS];
		private final float[] speed = new float[GROUPS];
		private final float[] phase = new float[GROUPS];
		private int width = -1;
		private int height = -1;
		private float density = Float.NaN;
		private int color;

		StarField(String name, int seed, int areaPerStar, boolean withGlow, boolean upperSky) {
			this.name = name;
			this.seed = seed;
			this.areaPerStar = areaPerStar;
			this.withGlow = withGlow;
			this.upperSky = upperSky;
			for (int g = 0; g < GROUPS; g++) {
				phase[g] = (g + 0.5f) / GROUPS * 6.283f;
				speed[g] = 0.6f + NightTheme.hash(g, 4, seed) * 1.8f;
			}
		}

		void extract(GuiGraphicsExtractor graphics, int w, int h, float starDensity, int starColor, double time) {
			if (w <= 0 || h <= 0) {
				return;
			}
			if (w != width || h != height || starDensity != density || starColor != color) {
				rebuild(w, h, starDensity, starColor);
			}
			for (int g = 0; g < GROUPS; g++) {
				float twinkle = (float) (0.5 + 0.5 * Math.sin(time * speed[g] + phase[g]));
				int alpha = (int) (60 + 195 * twinkle);
				graphics.blit(RenderPipelines.GUI_TEXTURED, stars[g], 0, 0, 0, 0, w, h, w, h, ARGB.color(alpha, 0xFFFFFF));
				if (withGlow && twinkle > 0.75f) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, glows[g], 0, 0, 0, 0, w, h, w, h, ARGB.color(alpha / 3, 0xFFFFFF));
				}
			}
		}

		private void rebuild(int w, int h, float starDensity, int starColor) {
			for (int g = 0; g < GROUPS; g++) {
				BakedTextures.release(stars[g]);
				BakedTextures.release(glows[g]);
				stars[g] = null;
				glows[g] = null;
			}
			width = w;
			height = h;
			density = starDensity;
			color = starColor;
			int pixel = starColor | 0xFF000000;
			NativeImage[] starImages = new NativeImage[GROUPS];
			NativeImage[] glowImages = new NativeImage[GROUPS];
			for (int g = 0; g < GROUPS; g++) {
				starImages[g] = new NativeImage(w, h, true);
				if (withGlow) {
					glowImages[g] = new NativeImage(w, h, true);
				}
			}
			int count = (int) (w * h / areaPerStar * Math.max(0.2f, starDensity));
			for (int i = 0; i < count; i++) {
				int x = (int) (NightTheme.hash(i, 1, seed) * w);
				int y;
				if (upperSky) {
					float fy = NightTheme.hash(i, 2, seed);
					y = (int) (fy * fy * h * 0.8f);
				} else {
					y = (int) (NightTheme.hash(i, 2, seed) * h);
				}
				if (x < 0 || x >= w || y < 0 || y >= h) {
					continue;
				}
				// Gruppe nach der ursprünglichen Funkel-Phase des Sterns
				int g = Math.min(GROUPS - 1, (int) (NightTheme.hash(i, 3, seed) * GROUPS));
				starImages[g].setPixel(x, y, pixel);
				if (withGlow && i % 9 == 0) {
					setIfInside(glowImages[g], x - 1, y, pixel);
					setIfInside(glowImages[g], x + 1, y, pixel);
					setIfInside(glowImages[g], x, y - 1, pixel);
					setIfInside(glowImages[g], x, y + 1, pixel);
				}
			}
			for (int g = 0; g < GROUPS; g++) {
				stars[g] = BakedTextures.upload(name, starImages[g]);
				if (withGlow) {
					glows[g] = BakedTextures.upload(name + "_glow", glowImages[g]);
				}
			}
		}

		private static void setIfInside(NativeImage image, int x, int y, int color) {
			if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
				image.setPixel(x, y, color);
			}
		}
	}
}
