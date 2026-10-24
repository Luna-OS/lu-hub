package dev.lunaos.luhub.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Welcher Arbeitsblock welchen Beruf gibt. */
public final class ProfessionDisplay extends BasicDisplay {
	public static final CategoryIdentifier<ProfessionDisplay> ID = CategoryIdentifier.of("luhub", "villager_professions");
	static final DisplaySerializer<ProfessionDisplay> SERIALIZER = ClientOnlySerializer.create();

	public final VillagerTradeData.Profession profession;

	public ProfessionDisplay(VillagerTradeData.Profession profession) {
		super(inputs(profession), List.of(EntryIngredients.of(Items.VILLAGER_SPAWN_EGG)));
		this.profession = profession;
	}

	private static List<EntryIngredient> inputs(VillagerTradeData.Profession profession) {
		List<EntryIngredient> inputs = new ArrayList<>();
		inputs.add(EntryIngredients.ofItemStacks(profession.jobSites()));
		profession.requested().forEach(stack -> inputs.add(EntryIngredients.of(stack)));
		return inputs;
	}

	public int tradeCount() {
		return profession.trades().values().stream().mapToInt(List::size).sum();
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
