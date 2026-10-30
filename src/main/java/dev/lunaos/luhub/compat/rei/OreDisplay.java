package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.ore.OreData;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.List;

/** Ein Erz (ore != null) oder die Übersicht aller Erze einer Dimension. */
public final class OreDisplay extends BasicDisplay {
	public static final CategoryIdentifier<OreDisplay> ID = CategoryIdentifier.of("luhub", "ore_heights");
	static final DisplaySerializer<OreDisplay> SERIALIZER = ClientOnlySerializer.create();

	public final OreData ore;
	public final OreData.Dim dim;

	public OreDisplay(OreData ore, OreData.Dim dim) {
		super(inputs(ore, dim), outputs(ore, dim));
		this.ore = ore;
		this.dim = dim;
	}

	private static List<OreData> ores(OreData ore, OreData.Dim dim) {
		return ore != null ? List.of(ore) : OreData.of(dim);
	}

	private static List<EntryIngredient> inputs(OreData ore, OreData.Dim dim) {
		return ores(ore, dim).stream().flatMap(o -> o.blocks.stream()).map(EntryIngredients::of).toList();
	}

	/** Die Übersicht hat keine Ausgabe – so zeigt "Rezepte für Diamant" direkt die Diamant-Seite. */
	private static List<EntryIngredient> outputs(OreData ore, OreData.Dim dim) {
		return ore != null ? List.of(EntryIngredients.of(ore.drop)) : List.of();
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
