package dev.lunaos.luhub.theme;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.resources.Identifier;

/**
 * Färbt Vanilla-GUI-Texturen beim Laden in das Nachthimmel-Theme um.
 * <p>
 * Die grauen Vanilla-Töne (Hintergrund #C6C6C6, Slots #8B8B8B, Kanten #FFFFFF/#555555/#373737, Rand #000000)
 * werden auf die Theme-Palette abgebildet, auf freien Hintergrund-Pixeln werden Sterne verteilt.
 * Farbige Pixel (Feuer, Pfeile, Tränke ...) bleiben unverändert. Weil die Texturen erst im Spiel
 * aus den Original-Ressourcen erzeugt werden, funktionieren auch Resource-Packs darunter weiter.
 */
public final class NightTheme {
	private static final int VANILLA_BACKGROUND = 0xC6C6C6;

	private NightTheme() {
	}

	/** Soll eine eigenständige Textur (z.B. textures/gui/container/furnace.png) umgefärbt werden? */
	public static boolean shouldThemeTexture(Identifier id) {
		LuConfig c = LuConfig.get();
		if (!c.themeEnabled) {
			return false;
		}
		String path = id.getPath();
		boolean vanilla = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
		if (path.startsWith("textures/gui/container/")) {
			return vanilla || c.themeModdedContainers;
		}
		return vanilla && c.themeRecipeBook && path.equals("textures/gui/recipe_book.png");
	}

	/** Soll ein Sprite aus dem GUI-Atlas (z.B. container/slot/sword, hud/hotbar) umgefärbt werden? */
	public static boolean shouldThemeSprite(Identifier id) {
		LuConfig c = LuConfig.get();
		if (!c.themeEnabled) {
			return false;
		}
		String path = id.getPath();
		boolean vanilla = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
		if (path.startsWith("container/")) {
			return vanilla || c.themeModdedContainers;
		}
		if (!vanilla) {
			return false;
		}
		if (path.startsWith("hud/hotbar")) {
			return c.themeHotbar;
		}
		return c.themeRecipeBook && path.startsWith("recipe_book/");
	}

	public static void apply(NativeImage image, Identifier id) {
		if (image.format() != NativeImage.Format.RGBA) {
			return;
		}
		LuConfig c = LuConfig.get();
		int[] anchorsGray = {0, 55, 85, 139, 198, 255};
		int[] anchorsColor = {c.colorBorder, c.colorShadow, c.colorShadow, c.colorSlot, c.colorBackground, c.colorHighlight};
		// Die dunkle Kante (#555555) etwas heller als der Slot-Schatten (#373737), damit die 3D-Optik bleibt
		anchorsColor[2] = lerp(c.colorShadow, c.colorSlot, 0.6f);

		int seed = id.hashCode();
		boolean stars = c.stars && c.starDensity > 0;
		float density = c.starDensity;
		int dimStar = lerp(c.colorBackground, c.colorHighlight, 0.45f);
		int faintStar = lerp(c.colorBackground, c.colorHighlight, 0.15f);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getPixel(x, y);
				int a = argb >>> 24;
				if (a == 0) {
					continue;
				}
				int r = (argb >> 16) & 0xFF;
				int g = (argb >> 8) & 0xFF;
				int b = argb & 0xFF;
				int max = Math.max(r, Math.max(g, b));
				int min = Math.min(r, Math.min(g, b));
				int lum = (r * 30 + g * 59 + b * 11) / 100;
				boolean grayish = max - min <= 12 || (lum < 60 && max - min <= 40);
				if (!grayish) {
					continue;
				}
				int color = mapGray(lum, anchorsGray, anchorsColor);
				if (stars && (argb & 0xFFFFFF) == VANILLA_BACKGROUND) {
					float v = hash(x, y, seed);
					if (v < 0.006f * density) {
						color = c.colorStar;
					} else if (v < 0.016f * density) {
						color = dimStar;
					} else if (v < 0.032f * density) {
						color = faintStar;
					}
				}
				image.setPixel(x, y, (a << 24) | (color & 0xFFFFFF));
			}
		}
	}

	private static int mapGray(int gray, int[] grays, int[] colors) {
		for (int i = 1; i < grays.length; i++) {
			if (gray <= grays[i]) {
				float t = (gray - grays[i - 1]) / (float) (grays[i] - grays[i - 1]);
				return lerp(colors[i - 1], colors[i], t);
			}
		}
		return colors[colors.length - 1];
	}

	public static int lerp(int c0, int c1, float t) {
		int a = (int) ((c0 >>> 24) + ((c1 >>> 24) - (c0 >>> 24)) * t);
		int r = (int) (((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * t);
		int g = (int) (((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * t);
		int b = (int) ((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** Deterministisches Rauschen in [0, 1) – gleiche Sterne bei jedem Laden. */
	public static float hash(int x, int y, int seed) {
		int h = seed ^ (x * 0x27D4EB2D) ^ (y * 0x165667B1);
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		h *= 0x297A2D39;
		h ^= h >>> 15;
		return (h >>> 8) / (float) (1 << 24);
	}
}
