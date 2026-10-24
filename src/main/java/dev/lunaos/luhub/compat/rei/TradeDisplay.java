package dev.lunaos.luhub.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Ein Handelsangebot eines Berufs auf einer Stufe. */
public final class TradeDisplay extends BasicDisplay {
	public static final CategoryIdentifier<TradeDisplay> ID = CategoryIdentifier.of("luhub", "villager_trades");
	static final DisplaySerializer<TradeDisplay> SERIALIZER = ClientOnlySerializer.create();

	public final VillagerTradeData.Profession profession;
	public final int level;
	public final VillagerTradeData.Trade trade;

	public TradeDisplay(VillagerTradeData.Profession profession, int level, VillagerTradeData.Trade trade) {
		super(inputs(profession, trade), List.of(EntryIngredients.of(trade.gives())));
		this.profession = profession;
		this.level = level;
		this.trade = trade;
	}

	private static List<EntryIngredient> inputs(VillagerTradeData.Profession profession, VillagerTradeData.Trade trade) {
		List<EntryIngredient> inputs = new ArrayList<>();
		inputs.add(EntryIngredients.of(trade.wants()));
		if (!trade.additional().isEmpty()) {
			inputs.add(EntryIngredients.of(trade.additional()));
		}
		if (!profession.jobSites().isEmpty()) {
			inputs.add(EntryIngredients.ofItemStacks(profession.jobSites()));
		}
		return inputs;
	}

	public List<ItemStack> jobSites() {
		return profession.jobSites();
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return ID;
	}

	@Override
	public DisplaySerializer<? extends me.shedaniel.rei.api.common.display.Display> getSerializer() {
		return SERIALIZER;
	}
}
