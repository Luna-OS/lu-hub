package dev.lunaos.luhub.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** "Dorfbewohner-Berufe": Arbeitsblock → Beruf, dazu die Items, die der Beruf sammelt. */
public final class ProfessionCategory implements DisplayCategory<ProfessionDisplay> {
	@Override
	public CategoryIdentifier<? extends ProfessionDisplay> getCategoryIdentifier() {
		return ProfessionDisplay.ID;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("luhub.villager.professions");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Items.VILLAGER_SPAWN_EGG);
	}

	@Override
	public int getDisplayHeight() {
		return 52;
	}

	@Override
	public int getDisplayWidth(ProfessionDisplay display) {
		return 150;
	}

	@Override
	public List<Widget> setupDisplay(ProfessionDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		widgets.add(ReiTheme.panel(bounds, false));
		int x = bounds.x + 6;
		int y = bounds.y + 5;
		widgets.add(Widgets.createLabel(new Point(x, y), display.profession.name()).leftAligned().color(ReiTheme.label(), ReiTheme.label()));
		int slotY = y + 15;
		widgets.add(ReiTheme.slot(x, slotY - 1));
		widgets.add(Widgets.createSlot(new Point(x + 1, slotY))
			.entries(display.profession.jobSites().stream().map(EntryStacks::of).toList()).disableBackground().markInput());
		widgets.add(Widgets.createArrow(new Point(x + 24, slotY)));
		widgets.add(ReiTheme.slot(x + 55, slotY - 1));
		widgets.add(Widgets.createSlot(new Point(x + 56, slotY)).disableBackground().entry(EntryStacks.of(new ItemStack(Items.VILLAGER_SPAWN_EGG))
			.tooltip(display.profession.name().copy().withStyle(ChatFormatting.GREEN))).markOutput());
		List<ItemStack> requested = display.profession.requested();
		for (int i = 0; i < requested.size() && i < 3; i++) {
			widgets.add(ReiTheme.slot(x + 81 + i * 18, slotY - 1));
			widgets.add(Widgets.createSlot(new Point(x + 82 + i * 18, slotY)).disableBackground().entry(EntryStacks.of(requested.get(i))
				.tooltip(Component.translatable("luhub.villager.requested").withStyle(ChatFormatting.GRAY))).markInput());
		}
		widgets.add(Widgets.createLabel(new Point(x, slotY + 21), Component.translatable("luhub.villager.trade_count", display.tradeCount()))
			.leftAligned().color(ReiTheme.dim(), ReiTheme.dim()).noShadow());
		return widgets;
	}
}
