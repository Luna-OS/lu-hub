package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import dev.lunaos.luhub.theme.ThemeDecor;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;

/** Nachthimmel-Look für die Lu-Hub-Seiten in REI – passend zu den Fenstern im Spiel. */
final class ReiTheme {
	private ReiTheme() {
	}

	/** Hintergrund mit Rahmen, Sternen und (bei großen Seiten) einem kleinen Mond. */
	static Widget panel(Rectangle bounds, boolean moon) {
		return Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> extractPanel(graphics, bounds, moon));
	}

	static void extractPanel(GuiGraphicsExtractor graphics, Rectangle b, boolean moon) {
		LuConfig c = LuConfig.get();
		graphics.fill(b.x, b.y, b.x + b.width, b.y + b.height, c.colorBorder);
		graphics.fillGradient(b.x + 1, b.y + 1, b.x + b.width - 1, b.y + b.height - 1, c.colorSkyTop, c.colorBackground);
		int stars = b.width * b.height / 700;
		int seed = b.width * 31 + b.height;
		for (int i = 0; i < stars; i++) {
			int sx = b.x + 2 + (int) (NightTheme.hash(i, 1, seed) * (b.width - 4));
			int sy = b.y + 2 + (int) (NightTheme.hash(i, 2, seed) * (b.height - 4));
			graphics.fill(sx, sy, sx + 1, sy + 1, ARGB.color(90 + (int) (NightTheme.hash(i, 3, seed) * 120), c.colorStar & 0xFFFFFF));
		}
		if (moon) {
			ThemeDecor.extractMoon(graphics, b.x + b.width - ThemeDecor.MOON_SIZE - 3, b.y + 3, ThemeDecor.MOON_SIZE, c.colorMoon, false);
		}
	}

	/** Slot-Hintergrund im Theme (18×18, Item bei x+1/y+1). */
	static Widget slot(int x, int y) {
		return Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			LuConfig c = LuConfig.get();
			graphics.fill(x, y, x + 18, y + 18, c.colorBorder);
			graphics.fill(x + 1, y + 1, x + 17, y + 17, c.colorSlot);
		});
	}

	/** Größerer Ergebnis-Slot (26×26, Item bei x+5/y+5). */
	static Widget resultSlot(int x, int y) {
		return Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			LuConfig c = LuConfig.get();
			graphics.fill(x, y, x + 26, y + 26, c.colorBorder);
			graphics.fill(x + 1, y + 1, x + 25, y + 25, c.colorSlot);
			graphics.outline(x + 1, y + 1, 24, 24, ARGB.color(0x60, c.colorHighlight & 0xFFFFFF));
		});
	}

	static int label() {
		return LuConfig.get().colorLabel;
	}

	static int dim() {
		return NightTheme.lerp(LuConfig.get().colorLabel, 0xFF000000, 0.3f);
	}
}
