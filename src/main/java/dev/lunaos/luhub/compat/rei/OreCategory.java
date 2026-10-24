package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * "Erz-Höhen": für jedes Erz die Verteilung über die Höhe als Kurve – im Nachthimmel-Stil von Lu Hub,
 * dazu die beste Höhe, ein guter Abbau-Bereich und ein Tipp. Die Übersicht zeigt alle Erze nebeneinander.
 */
public final class OreCategory implements DisplayCategory<OreDisplay> {
	private static final int WIDTH = 166;
	private static final int HEIGHT = 140;

	@Override
	public CategoryIdentifier<? extends OreDisplay> getCategoryIdentifier() {
		return OreDisplay.ID;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("luhub.ores.title");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Items.DEEPSLATE_DIAMOND_ORE);
	}

	@Override
	public int getDisplayHeight() {
		return HEIGHT;
	}

	@Override
	public int getDisplayWidth(OreDisplay display) {
		return WIDTH;
	}

	@Override
	public int getMaximumDisplaysPerPage() {
		return 1;
	}

	@Override
	public List<Widget> setupDisplay(OreDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		int x = bounds.x;
		int y = bounds.y;
		widgets.add(ReiTheme.panel(bounds, true));
		if (display.ore != null) {
			setupSingle(widgets, display.ore, x, y);
		} else {
			setupOverview(widgets, display.dim, x, y);
		}
		return widgets;
	}

	// ------------------------------------------------------------------ einzelnes Erz

	private void setupSingle(List<Widget> widgets, OreData ore, int x, int y) {
		LuConfig c = LuConfig.get();
		int chartX = x + 108;
		int chartY = y + 18;
		int chartW = 52;
		int chartH = HEIGHT - 24;
		int slotY = y + 18;
		for (int i = 0; i < ore.blocks.size(); i++) {
			widgets.add(ReiTheme.slot(x + 6 + i * 19, slotY));
			widgets.add(Widgets.createSlot(new Point(x + 7 + i * 19, slotY + 1)).entry(EntryStacks.of(ore.blocks.get(i))).disableBackground().markInput());
		}
		int arrowX = x + 8 + ore.blocks.size() * 19;
		widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			Font font = Minecraft.getInstance().font;
			graphics.text(font, ore.label(), x + 6, y + 6, ore.color, true);
			graphics.text(font, "→", arrowX + 2, slotY + 5, c.colorLabel, false);
			int infoY = y + 42;
			graphics.text(font, Component.translatable("luhub.ores.best"), x + 6, infoY, ReiTheme.dim(), false);
			var pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(x + 6, infoY + 10);
			pose.scale(1.5f, 1.5f);
			graphics.text(font, "Y = " + ore.bestY, 0, 0, ore.color, true);
			pose.popMatrix();
			graphics.text(font, Component.translatable("luhub.ores.range", ore.rangeMin, ore.rangeMax), x + 6, infoY + 24, c.colorLabel, false);
			// Tipp etwas kleiner, damit er ganz hineinpasst
			float scale = 0.85f;
			List<FormattedCharSequence> tip = font.split(ore.tip(), Math.round(98 / scale));
			pose.pushMatrix();
			pose.translate(x + 6, infoY + 37);
			pose.scale(scale, scale);
			for (int i = 0; i < Math.min(tip.size(), 7); i++) {
				graphics.text(font, tip.get(i), 0, i * 9, 0xFFC9CDEE, false);
			}
			pose.popMatrix();
			extractChart(graphics, List.of(ore), ore.dim, chartX, chartY, chartW, chartH, mouseX, mouseY, true);
		}));
		widgets.add(ReiTheme.slot(arrowX + 14, slotY));
		widgets.add(Widgets.createSlot(new Point(arrowX + 15, slotY + 1)).entry(EntryStacks.of(ore.drop)).disableBackground().markOutput());
		widgets.add(Widgets.createTooltip(point -> chartTooltip(point, List.of(ore), ore.dim, chartX, chartY, chartW, chartH)));
	}

	// ------------------------------------------------------------------ Übersicht

	private void setupOverview(List<Widget> widgets, OreData.Dim dim, int x, int y) {
		List<OreData> ores = OreData.of(dim);
		int chartX = x + 26;
		int chartY = y + 38;
		int chartW = WIDTH - 32;
		int chartH = HEIGHT - 44;
		int col = chartW / ores.size();
		for (int i = 0; i < ores.size(); i++) {
			int sx = chartX + i * col + (col - 16) / 2;
			widgets.add(Widgets.createSlot(new Point(sx, y + 18)).entry(EntryStacks.of(ores.get(i).blocks.getLast())).disableBackground().markInput());
		}
		widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			Font font = Minecraft.getInstance().font;
			graphics.text(font, Component.translatable("luhub.ores.overview", dim.label()), x + 6, y + 6, LuConfig.get().colorLabel, true);
			extractChart(graphics, ores, dim, chartX, chartY, chartW, chartH, mouseX, mouseY, false);
			// Höhen-Beschriftung links
			for (int label : axisLabels(dim)) {
				int ly = rowOf(label, dim, chartY, chartH);
				String text = String.valueOf(label);
				graphics.text(font, text, chartX - 3 - font.width(text), ly - 4, 0xFF9AA4D8, false);
			}
		}));
		widgets.add(Widgets.createTooltip(point -> chartTooltip(point, ores, dim, chartX, chartY, chartW, chartH)));
	}

	private static int[] axisLabels(OreData.Dim dim) {
		return dim == OreData.Dim.OVERWORLD ? new int[]{320, 192, 64, 0, -64} : new int[]{128, 64, 0};
	}

	// ------------------------------------------------------------------ Zeichnen



	private static int rowOf(int worldY, OreData.Dim dim, int chartY, int chartH) {
		float t = (dim.maxY - worldY) / (float) (dim.maxY - dim.minY);
		return chartY + Math.round(t * (chartH - 1));
	}

	private static int worldYOf(int row, OreData.Dim dim, int chartY, int chartH) {
		float t = (row - chartY + 0.5f) / chartH;
		return Math.round(dim.maxY - t * (dim.maxY - dim.minY));
	}

	/**
	 * Die Verteilungskurven: je Erz eine Spalte, symmetrisch um die Spaltenmitte (wie die bekannte 1.18-Grafik).
	 * Biom-/höhlenabhängige Anteile sind schraffiert, der gute Abbau-Bereich ist hinterlegt.
	 */
	private static void extractChart(GuiGraphicsExtractor graphics, List<OreData> ores, OreData.Dim dim, int x, int y, int w, int h,
		int mouseX, int mouseY, boolean single) {
		LuConfig c = LuConfig.get();
		// Himmel → Stein → Tiefenschiefer (bzw. Nether-Rot)
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
			graphics.fill(x, lava, x + w, y + h, 0x30FF6A1A); // Lavasee-Höhe
		}
		graphics.fill(x, y + h - 2, x + w, y + h, 0xFF101018); // Grundgestein
		for (int label : axisLabels(dim)) {
			int ly = rowOf(label, dim, y, h);
			graphics.fill(x, ly, x + w, ly + 1, 0x18FFFFFF);
		}
		int col = w / ores.size();
		for (int i = 0; i < ores.size(); i++) {
			OreData ore = ores.get(i);
			int cx = x + i * col + col / 2;
			float max = ore.maxDensity();
			int maxHalf = col / 2 - 2;
			if (single) {
				int r0 = rowOf(ore.rangeMax, dim, y, h);
				int r1 = rowOf(ore.rangeMin, dim, y, h);
				graphics.fill(x, r0, x + w, r1 + 1, ARGB.color(0x30, ore.color & 0xFFFFFF));
			}
			for (int row = y; row < y + h - 2; row++) {
				int wy = worldYOf(row, dim, y, h);
				float normal = ore.density(wy, false);
				float special = ore.density(wy, true);
				if (special > 0f) {
					int half = Math.round((float) Math.sqrt((normal + special) / max) * maxHalf);
					if (half > 0 && (row & 1) == 0) {
						graphics.fill(cx - half, row, cx + half, row + 1, ARGB.color(0x70, ore.color & 0xFFFFFF));
					}
				}
				if (normal > 0f) {
					int half = Math.max(1, Math.round((float) Math.sqrt(normal / max) * maxHalf));
					float shade = 0.75f + 0.25f * (float) Math.sqrt(normal / max);
					graphics.fill(cx - half, row, cx + half, row + 1, ARGB.color(0xE6, NightTheme.lerp(0xFF000000, ore.color, shade) & 0xFFFFFF));
				}
			}
			// beste Höhe
			int best = rowOf(ore.bestY, dim, y, h);
			int left = single ? x : x + i * col + 1;
			int right = single ? x + w : x + (i + 1) * col - 1;
			graphics.fill(left, best, right, best + 1, 0xE0FFFFFF);
			graphics.fill(right - 3, best - 2, right, best + 3, ore.color);
		}
		// Maus-Linie
		if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
			graphics.fill(x, mouseY, x + w, mouseY + 1, 0x50FFFFFF);
		}
	}

	private static Tooltip chartTooltip(Point point, List<OreData> ores, OreData.Dim dim, int x, int y, int w, int h) {
		if (point.x < x || point.x >= x + w || point.y < y || point.y >= y + h) {
			return null;
		}
		int wy = worldYOf(point.y, dim, y, h);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("Y = " + wy));
		List<OreData> shown = ores;
		if (ores.size() > 1) {
			int col = w / ores.size();
			int index = Math.min(ores.size() - 1, (point.x - x) / col);
			shown = List.of(ores.get(index));
		}
		for (OreData ore : shown) {
			float rel = (ore.density(wy, false) + ore.density(wy, true)) / ore.maxDensity();
			lines.add(Component.empty().append(ore.label()).append(": ")
				.append(String.format(Locale.ROOT, "%d %%", Math.round(rel * 100))).withColor(ore.color & 0xFFFFFF));
			if (ores.size() > 1) {
				lines.add(Component.translatable("luhub.ores.best_short", ore.bestY).withColor(0xC9CDEE));
			}
			if (ore.density(wy, true) > 0f) {
				lines.add(Component.translatable("luhub.ores." + ore.name().toLowerCase(Locale.ROOT) + ".special").withColor(0x9AA4D8));
			}
		}
		return Tooltip.create(point, lines);
	}
}
