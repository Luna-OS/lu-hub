package dev.lunaos.luhub.screen;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.ore.OreChart;
import dev.lunaos.luhub.ore.OreData;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Erz-Höhen auch ohne REI: links die Erze (und je eine Übersicht für Oberwelt und Nether), rechts die Grafik –
 * wo es am meisten und wo am wenigsten gibt, die perfekte Höhe und die eigene Höhe.
 */
public final class OreHeightScreen extends Screen {
	private static final int ROW = 15;
	private static final int LIST_WIDTH = 118;

	/** Ein Eintrag der Liste: ein Erz oder die Übersicht einer Dimension (ore == null). */
	private record Entry(OreData ore, OreData.Dim dim, ItemStack icon, Component label) {
	}

	private static Entry lastSelected;

	private final Screen parent;
	private final List<Entry> entries = new ArrayList<>();
	private Entry selected;
	private int panelX;
	private int panelY;
	private int panelW;
	private int panelH;

	public OreHeightScreen(Screen parent) {
		super(Component.translatable("luhub.ores.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		entries.clear();
		for (OreData.Dim dim : OreData.Dim.values()) {
			ItemStack overviewIcon = new ItemStack(dim == OreData.Dim.OVERWORLD ? Items.IRON_PICKAXE : Items.NETHERRACK);
			entries.add(new Entry(null, dim, overviewIcon, Component.translatable("luhub.ores.list_overview", dim.label())));
			for (OreData ore : OreData.of(dim)) {
				entries.add(new Entry(ore, dim, new ItemStack(ore.blocks.getLast()), ore.label()));
			}
		}
		selected = null;
		if (lastSelected != null) {
			for (Entry entry : entries) {
				if (entry.ore() == lastSelected.ore() && entry.dim() == lastSelected.dim()) {
					selected = entry;
				}
			}
		}
		if (selected == null) {
			// Standard: Übersicht der Dimension, in der man gerade ist
			boolean nether = minecraft.level != null && minecraft.level.dimension() == Level.NETHER;
			OreData.Dim dim = nether ? OreData.Dim.NETHER : OreData.Dim.OVERWORLD;
			selected = entries.stream().filter(e -> e.ore() == null && e.dim() == dim).findFirst().orElse(entries.getFirst());
		}
		panelW = Math.min(width - 16, 400);
		panelH = Math.min(height - 12, entries.size() * ROW + 36);
		panelX = (width - panelW) / 2;
		panelY = (height - panelH) / 2;
	}

	private int listTop() {
		return panelY + 24;
	}

	private Entry entryAt(double mx, double my) {
		int left = panelX + 6;
		if (mx < left || mx >= left + LIST_WIDTH || my < listTop()) {
			return null;
		}
		int index = (int) ((my - listTop()) / ROW);
		return index >= 0 && index < entries.size() ? entries.get(index) : null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		LuConfig c = LuConfig.get();
		// Fenster im Nachthimmel-Stil
		graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, c.colorBorder);
		graphics.fillGradient(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, c.colorSkyTop, c.colorBackground);
		ThemeDecor.extractMoon(graphics, panelX + panelW - ThemeDecor.MOON_SIZE - 5, panelY + 5, ThemeDecor.MOON_SIZE, c.colorMoon, false);
		graphics.text(font, title, panelX + 8, panelY + 8, c.colorLabel, true);

		// Liste
		Entry hovered = entryAt(mouseX, mouseY);
		int x = panelX + 6;
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int y = listTop() + i * ROW;
			if (y + ROW > panelY + panelH) {
				break;
			}
			if (entry == selected) {
				graphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, (c.colorHighlight & 0xFFFFFF) | 0x50000000);
				graphics.outline(x, y, LIST_WIDTH, ROW - 1, c.colorHighlight);
			} else if (entry == hovered) {
				graphics.fill(x, y, x + LIST_WIDTH, y + ROW - 1, 0x30FFFFFF);
			}
			graphics.item(entry.icon(), x + 1, y - 1);
			int color = entry.ore() != null ? entry.ore().color : c.colorLabel;
			String label = font.plainSubstrByWidth(entry.label().getString(), LIST_WIDTH - 22);
			graphics.text(font, label, x + 20, y + 3, color, false);
		}

		// Grafik
		int areaX = panelX + LIST_WIDTH + 14;
		int areaRight = panelX + panelW - 8;
		int chartTop = panelY + 24;
		int chartH = panelY + panelH - 10 - chartTop;
		List<Component> tooltip;
		if (selected.ore() != null) {
			OreData ore = selected.ore();
			int infoW = Math.min(110, (areaRight - areaX) / 2);
			graphics.text(font, ore.label(), areaX, chartTop, ore.color, true);
			for (int i = 0; i < ore.blocks.size(); i++) {
				graphics.item(new ItemStack(ore.blocks.get(i)), areaX + i * 18, chartTop + 11);
			}
			graphics.text(font, "→", areaX + ore.blocks.size() * 18 + 2, chartTop + 15, c.colorLabel, false);
			graphics.item(new ItemStack(ore.drop), areaX + ore.blocks.size() * 18 + 12, chartTop + 11);
			OreChart.extractInfo(graphics, ore, areaX, chartTop + 32, infoW, panelY + panelH - 22);
			OreChart.extractLegend(graphics, ore, areaX, panelY + panelH - 18, 70);
			int chartX = areaX + infoW + 22;
			int chartW = Math.max(30, areaRight - chartX);
			OreChart.extractAxis(graphics, ore.dim, chartX - 3, chartTop, chartH, 0.75f);
			OreChart.extract(graphics, List.of(ore), ore.dim, chartX, chartTop, chartW, chartH, mouseX, mouseY, true);
			tooltip = OreChart.tooltip(mouseX, mouseY, List.of(ore), ore.dim, chartX, chartTop, chartW, chartH);
		} else {
			List<OreData> ores = OreData.of(selected.dim());
			int chartX = areaX + 22;
			int chartW = areaRight - chartX;
			int col = chartW / ores.size();
			for (int i = 0; i < ores.size(); i++) {
				graphics.item(new ItemStack(ores.get(i).blocks.getLast()), chartX + i * col + (col - 16) / 2, chartTop);
			}
			int top = chartTop + 20;
			int h = chartH - 20;
			OreChart.extractAxis(graphics, selected.dim(), chartX - 3, top, h, 0.75f);
			OreChart.extract(graphics, ores, selected.dim(), chartX, top, chartW, h, mouseX, mouseY, false);
			tooltip = OreChart.tooltip(mouseX, mouseY, ores, selected.dim(), chartX, top, chartW, h);
		}
		if (tooltip != null) {
			graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Entry entry = entryAt(event.x(), event.y());
		if (entry != null) {
			selected = entry;
			lastSelected = entry;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		// Mausrad blättert durch die Erze
		int index = entries.indexOf(selected) + (scrollY > 0 ? -1 : 1);
		selected = entries.get(Math.floorMod(index, entries.size()));
		lastSelected = selected;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
