package dev.lunaos.luhub.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** "Dorfbewohner-Handel": was ein Beruf auf welcher Stufe kauft und verkauft. */
public final class TradeCategory implements DisplayCategory<TradeDisplay> {
	@Override
	public CategoryIdentifier<? extends TradeDisplay> getCategoryIdentifier() {
		return TradeDisplay.ID;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("luhub.villager.trades");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(Items.EMERALD);
	}

	@Override
	public int getDisplayHeight() {
		return 46;
	}

	@Override
	public int getDisplayWidth(TradeDisplay display) {
		return 150;
	}

	static Component levelName(VillagerTradeData.Profession profession, int level) {
		return profession.jobSites().isEmpty()
			? Component.translatable("luhub.villager.wandering." + level)
			: Component.translatable("merchant.level." + level);
	}

	@Override
	public List<Widget> setupDisplay(TradeDisplay display, Rectangle bounds) {
		List<Widget> widgets = new ArrayList<>();
		widgets.add(ReiTheme.panel(bounds, false));
		int x = bounds.x + 6;
		int y = bounds.y + 5;
		widgets.add(Widgets.createLabel(new Point(x, y), Component.empty().append(display.profession.name())
			.append(Component.literal(" · ")).append(levelName(display.profession, display.level)))
			.leftAligned().color(ReiTheme.label(), ReiTheme.label()));
		int slotY = y + 17;
		VillagerTradeData.Trade trade = display.trade;
		widgets.add(ReiTheme.slot(x, slotY - 1));
		widgets.add(Widgets.createSlot(new Point(x + 1, slotY)).entry(cost(trade.wants(), trade.variableCost())).disableBackground().markInput());
		int arrowX = x + 26;
		if (!trade.additional().isEmpty()) {
			widgets.add(ReiTheme.slot(x + 20, slotY - 1));
			widgets.add(Widgets.createSlot(new Point(x + 21, slotY)).entry(cost(trade.additional(), false)).disableBackground().markInput());
			arrowX = x + 46;
		}
		widgets.add(Widgets.createArrow(new Point(arrowX, slotY)));
		EntryStack<ItemStack> gives = EntryStacks.of(trade.gives());
		if (trade.modified()) {
			gives = gives.tooltip(Component.translatable("luhub.villager.modified").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		widgets.add(ReiTheme.resultSlot(arrowX + 27, slotY - 5));
		widgets.add(Widgets.createSlot(new Point(arrowX + 32, slotY)).entry(gives).disableBackground().markOutput());
		if (!display.jobSites().isEmpty()) {
			widgets.add(ReiTheme.slot(bounds.getMaxX() - 23, slotY - 1));
			widgets.add(Widgets.createSlot(new Point(bounds.getMaxX() - 22, slotY))
				.entries(display.jobSites().stream().map(EntryStacks::of).toList()).disableBackground());
		}
		return widgets;
	}

	private static EntryStack<ItemStack> cost(ItemStack stack, boolean variable) {
		EntryStack<ItemStack> entry = EntryStacks.of(stack);
		return variable ? entry.tooltip(Component.translatable("luhub.villager.variable_cost").withStyle(ChatFormatting.GRAY)) : entry;
	}
}
