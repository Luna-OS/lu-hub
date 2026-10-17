package dev.lunaos.luhub.theme;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Färbt Vanilla-GUI-Texturen beim Laden in das Nachthimmel-Theme um.
 * <p>
 * Die grauen Vanilla-Töne (Hintergrund #C6C6C6, Slots #8B8B8B, Kanten #FFFFFF/#555555/#373737, Rand #000000,
 * Knöpfe #6F6F6F ...) werden auf die Theme-Palette abgebildet, auf freien Hintergrund-Pixeln werden Sterne verteilt.
 * Farbige Pixel (Feuer, Pfeile, Buchseiten ...) bleiben unverändert. Weil die Texturen erst im Spiel
 * aus den Original-Ressourcen erzeugt werden, funktionieren auch Resource-Packs darunter weiter.
 */
public final class NightTheme {
	private static final int VANILLA_BACKGROUND = 0xC6C6C6;

	/** Sprite-Ordner, die wie Container-Fenster aussehen (heller Hintergrund, eingelassene Slots). */
	private static final Set<String> PANEL_SPRITES = Set.of("container/", "recipe_book/", "advancements/", "statistics/");
	/** Sprite-Ordner mit Knöpfen, Reglern, Tooltips, Toasts ... */
	private static final Set<String> WIDGET_SPRITES = Set.of("widget/", "dialog/", "toast/", "popup/", "tooltip/",
		"gamemode_switcher/", "spectator/", "transferable_list/");
	/** Texturen, die farbig/aus Holz/Papier sind oder Logos – bleiben wie sie sind. */
	private static final Set<String> TEXTURE_EXCLUDES = Set.of("textures/gui/book.png", "textures/gui/title/",
		"textures/gui/signs/", "textures/gui/hanging_signs/", "textures/gui/presets/", "textures/gui/advancements/backgrounds/",
		"textures/gui/realms/");

	/** Wird bei jedem Neuladen von Theme-Texturen erhöht – davon abgeleitete Texturen bauen sich dann neu. */
	public static volatile int generation;

	private NightTheme() {
	}

	/** Wie ein Bild umgefärbt wird. */
	public enum Kind {
		/** Fenster mit Slots: Hintergrund #C6C6C6, Slots #8B8B8B. */
		PANEL(new int[]{0, 55, 85, 139, 198, 255}),
		/** Knöpfe &amp; Co.: Knopf #6F6F6F, deaktiviert #2C2C2C, helle Kante #A0A0A0. */
		WIDGET(new int[]{0, 44, 85, 111, 165, 255});

		final int[] grays;

		Kind(int[] grays) {
			this.grays = grays;
		}
	}

	/** Die Farben, auf die die Vanilla-Grautöne abgebildet werden. */
	public record Palette(int border, int shadow, int slot, int background, int highlight, int star) {
		public static Palette fromConfig() {
			LuConfig c = LuConfig.get();
			return new Palette(c.colorBorder, c.colorShadow, c.colorSlot, c.colorBackground, c.colorHighlight, c.colorStar);
		}

		/** Theme-Palette, eingefärbt in der Farbe einer Shulkerkiste (oder jeder anderen Farbe). */
		public static Palette tinted(int rgb) {
			LuConfig c = LuConfig.get();
			int dye = 0xFF000000 | rgb;
			int background = lerp(darken(dye, 0.35f), c.colorBackground, 0.3f);
			return new Palette(darken(dye, 0.88f), darken(background, 0.6f), darken(background, 0.45f), background,
				lerp(dye, 0xFFFFFFFF, 0.5f), c.colorStar);
		}

		int[] colors(Kind kind) {
			return switch (kind) {
				case PANEL -> new int[]{border, shadow, lerp(shadow, slot, 0.6f), slot, background, highlight};
				case WIDGET -> new int[]{border, darken(background, 0.45f), lerp(shadow, background, 0.5f), background,
					lerp(background, highlight, 0.45f), highlight};
			};
		}
	}

	private static boolean allGuis() {
		return LuConfig.get().themeAllGuis;
	}

	/** Soll eine eigenständige Textur (z.B. textures/gui/container/furnace.png) umgefärbt werden? */
	public static Kind textureKind(Identifier id) {
		LuConfig c = LuConfig.get();
		if (!c.themeEnabled) {
			return null;
		}
		String path = id.getPath();
		boolean vanilla = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
		if (path.startsWith("textures/gui/container/")) {
			return vanilla || c.themeModdedContainers ? Kind.PANEL : null;
		}
		if (!vanilla || !path.startsWith("textures/gui/") || path.startsWith("textures/gui/sprites/")) {
			return null;
		}
		if (path.equals("textures/gui/recipe_book.png")) {
			return c.themeRecipeBook ? Kind.PANEL : null;
		}
		if (!allGuis()) {
			return null;
		}
		for (String exclude : TEXTURE_EXCLUDES) {
			if (path.startsWith(exclude)) {
				return null;
			}
		}
		return Kind.PANEL;
	}

	/** Soll ein Sprite aus dem GUI-Atlas (z.B. container/slot/sword, widget/button) umgefärbt werden? */
	public static Kind spriteKind(Identifier id) {
		LuConfig c = LuConfig.get();
		if (!c.themeEnabled) {
			return null;
		}
		String path = id.getPath();
		boolean vanilla = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE);
		if (path.startsWith("container/")) {
			return vanilla || c.themeModdedContainers ? Kind.PANEL : null;
		}
		if (!vanilla) {
			return null;
		}
		if (path.startsWith("hud/hotbar")) {
			return c.themeHotbar ? Kind.PANEL : null;
		}
		if (path.startsWith("recipe_book/")) {
			return c.themeRecipeBook ? Kind.PANEL : null;
		}
		if (!allGuis()) {
			return null;
		}
		for (String prefix : PANEL_SPRITES) {
			if (path.startsWith(prefix)) {
				return Kind.PANEL;
			}
		}
		for (String prefix : WIDGET_SPRITES) {
			if (path.startsWith(prefix)) {
				return Kind.WIDGET;
			}
		}
		return null;
	}

	public static void apply(NativeImage image, Identifier id, Kind kind) {
		apply(image, id.hashCode(), kind, Palette.fromConfig());
	}

	public static void apply(NativeImage image, int seed, Kind kind, Palette palette) {
		if (image.format() != NativeImage.Format.RGBA) {
			return;
		}
		LuConfig c = LuConfig.get();
		int[] grays = kind.grays;
		int[] colors = palette.colors(kind);

		boolean stars = c.stars && c.starDensity > 0;
		float density = c.starDensity;
		int dimStar = lerp(palette.background(), palette.highlight(), 0.45f);
		int faintStar = lerp(palette.background(), palette.highlight(), 0.15f);

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
				int color = mapGray(lum, grays, colors);
				if (stars && a == 0xFF && isStarGround(kind, argb & 0xFFFFFF, lum)) {
					float v = hash(x, y, seed);
					float scale = kind == Kind.WIDGET ? 0.35f : 1f;
					if (v < 0.006f * density * scale) {
						color = palette.star();
					} else if (v < 0.016f * density * scale) {
						color = dimStar;
					} else if (v < 0.032f * density * scale) {
						color = faintStar;
					}
				}
				image.setPixel(x, y, (a << 24) | (color & 0xFFFFFF));
			}
		}
	}

	private static boolean isStarGround(Kind kind, int rgb, int lum) {
		return kind == Kind.PANEL ? rgb == VANILLA_BACKGROUND : lum >= 105 && lum <= 117;
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

	public static int darken(int color, float amount) {
		return lerp(color, color & 0xFF000000, amount);
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
