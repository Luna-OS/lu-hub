package dev.lunaos.luhub.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/** Für die REI-Anzeige der Dorfbewohner-Handel. */
@Mixin(VillagerTrade.class)
public interface VillagerTradeAccessor {
	@Accessor("wants")
	TradeCost luhub$getWants();

	@Accessor("additionalWants")
	Optional<TradeCost> luhub$getAdditionalWants();

	@Accessor("gives")
	ItemStackTemplate luhub$getGives();

	@Accessor("givenItemModifier")
	Optional<Holder<LootItemFunction>> luhub$getGivenItemModifier();
}
