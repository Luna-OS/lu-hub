package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.ore.OreChart;
import dev.lunaos.luhub.ore.OreData;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * "Erz-Höhen": für jedes Erz die Verteilung über die Höhe – wo es am meisten (hell leuchtend) und wo am wenigsten
 * (dunkel) gibt, die perfekte Höhe (★), ein guter Abbau-Bereich, die eigene Höhe und ein Tipp.
 * Die Übersicht zeigt alle Erze nebeneinander. Gezeichnet wird mit {@link OreChart}, wie im Erz-Höhen-Bildschirm.
 */
public final class OreCategory implements DisplayCategory<OreDisplay> {
	private static final int WIDTH = 176;
	private static final int HEIGHT = 162;

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
		widgets.add(ReiTheme.panel(bounds, true));
		if (display.ore != null) {
			setupSingle(widgets, display.ore, bounds.x, bounds.y);
		} else {
			setupOverview(widgets, display.dim, bounds.x, bounds.y);
		}
		return widgets;
	}

	private void setupSingle(List<Widget> widgets, OreData ore, int x, int y) {
		int chartX = x + 114;
		int chartY = y + 18;
		int chartW = 56;
		int chartH = HEIGHT - 24;
		int slotY = y + 18;
		for (int i = 0; i < ore.blocks.size(); i++) {
			widgets.add(ReiTheme.slot(x + 6 + i * 19, slotY));
			widgets.add(Widgets.createSlot(new Point(x + 7 + i * 19, slotY + 1)).entry(EntryStacks.of(ore.blocks.get(i))).disableBackground().markInput());
		}
		int arrowX = x + 8 + ore.blocks.size() * 19;
		widgets.add(ReiTheme.slot(arrowX + 14, slotY));
		widgets.add(Widgets.createSlot(new Point(arrowX + 15, slotY + 1)).entry(EntryStacks.of(ore.drop)).disableBackground().markOutput());
		widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			Font font = Minecraft.getInstance().font;
			graphics.text(font, ore.label(), x + 6, y + 6, ore.color, true);
			graphics.text(font, "→", arrowX + 2, slotY + 5, LuConfig.get().colorLabel, false);
			OreChart.extractInfo(graphics, ore, x + 6, y + 42, 96, y + HEIGHT - 16);
			OreChart.extractLegend(graphics, ore, x + 6, y + HEIGHT - 14, 64);
			OreChart.extractAxis(graphics, ore.dim, chartX - 2, chartY, chartH, 0.5f);
			OreChart.extract(graphics, List.of(ore), ore.dim, chartX, chartY, chartW, chartH, mouseX, mouseY, true);
		}));
		widgets.add(tooltip(List.of(ore), ore.dim, chartX, chartY, chartW, chartH));
	}

	private void setupOverview(List<Widget> widgets, OreData.Dim dim, int x, int y) {
		List<OreData> ores = OreData.of(dim);
		int chartX = x + 24;
		int chartY = y + 38;
		int chartW = WIDTH - 30;
		int chartH = HEIGHT - 44;
		int col = chartW / ores.size();
		for (int i = 0; i < ores.size(); i++) {
			int sx = chartX + i * col + (col - 16) / 2;
			widgets.add(Widgets.createSlot(new Point(sx, y + 18)).entry(EntryStacks.of(ores.get(i).blocks.getLast())).disableBackground().markInput());
		}
		widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) -> {
			Font font = Minecraft.getInstance().font;
			graphics.text(font, Component.translatable("luhub.ores.overview", dim.label()), x + 6, y + 6, LuConfig.get().colorLabel, true);
			OreChart.extractAxis(graphics, dim, chartX - 3, chartY, chartH, 0.75f);
			OreChart.extract(graphics, ores, dim, chartX, chartY, chartW, chartH, mouseX, mouseY, false);
		}));
		widgets.add(tooltip(ores, dim, chartX, chartY, chartW, chartH));
	}

	private static Widget tooltip(List<OreData> ores, OreData.Dim dim, int x, int y, int w, int h) {
		return Widgets.createTooltip(point -> {
			List<Component> lines = OreChart.tooltip(point.x, point.y, ores, dim, x, y, w, h);
			return lines == null ? null : Tooltip.create(point, lines);
		});
	}
}
