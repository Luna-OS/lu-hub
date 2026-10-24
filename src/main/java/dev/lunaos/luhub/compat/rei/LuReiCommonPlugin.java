package dev.lunaos.luhub.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import net.minecraft.resources.Identifier;

/** Meldet die (rein clientseitigen) Lu-Hub-Displays bei REI an. */
public final class LuReiCommonPlugin implements REICommonPlugin {
	@Override
	public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
		registry.register(Identifier.fromNamespaceAndPath("luhub", "ore_heights"), OreDisplay.SERIALIZER);
		registry.register(Identifier.fromNamespaceAndPath("luhub", "villager_trades"), TradeDisplay.SERIALIZER);
		registry.register(Identifier.fromNamespaceAndPath("luhub", "villager_professions"), ProfessionDisplay.SERIALIZER);
	}
}
