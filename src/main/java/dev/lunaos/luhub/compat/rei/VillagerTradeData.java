package dev.lunaos.luhub.compat.rei;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lunaos.luhub.LuHub;
import dev.lunaos.luhub.mixin.VillagerTradeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Berufe und Handelsangebote der Dorfbewohner für REI.
 * <p>
 * In 26.3 sind die Handel datengetrieben und liegen nur auf dem Server. Im Einzelspieler liest Lu Hub sie direkt
 * vom eingebauten Server (inklusive Datenpaketen); auf Servern werden die Vanilla-Handel aus dem Spiel selbst gebaut.
 */
public final class VillagerTradeData {
	/** Ein Handel: Preis (+ optional zweiter Preis) gegen Ware. */
	public record Trade(ItemStack wants, ItemStack additional, ItemStack gives, boolean variableCost, boolean modified) {
	}

	/** Ein Beruf mit Arbeitsblock, gewünschten Items und Handeln je Stufe (1 = Neuling ... 5 = Meister). */
	public record Profession(Identifier id, Component name, List<ItemStack> jobSites, List<ItemStack> requested,
		Map<Integer, List<Trade>> trades) {
	}

	private VillagerTradeData() {
	}

	public static List<Profession> load() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return List.of();
		}
		Function<ResourceKey<TradeSet>, List<VillagerTrade>> resolver = resolver(mc);
		List<Profession> result = new ArrayList<>();
		for (Holder.Reference<VillagerProfession> holder : BuiltInRegistries.VILLAGER_PROFESSION.listElements().toList()) {
			VillagerProfession profession = holder.value();
			List<ItemStack> jobSites = jobSites(profession);
			if (jobSites.isEmpty()) {
				continue; // "Arbeitslos" und "Nitwit" haben keinen Arbeitsblock
			}
			Map<Integer, List<Trade>> trades = new TreeMap<>();
			profession.tradeSetsByLevel().forEach((level, key) -> trades.put(level, convert(resolver.apply(key))));
			List<ItemStack> requested = profession.requestedItems().stream().map(ItemStack::new).toList();
			result.add(new Profession(holder.key().identifier(), profession.name(), jobSites, requested, trades));
		}
		// Fahrender Händler: Ankauf, häufige und seltene Angebote
		Map<Integer, List<Trade>> wandering = new TreeMap<>();
		wandering.put(1, convert(resolver.apply(TradeSets.WANDERING_TRADER_BUYING)));
		wandering.put(2, convert(resolver.apply(TradeSets.WANDERING_TRADER_COMMON)));
		wandering.put(3, convert(resolver.apply(TradeSets.WANDERING_TRADER_UNCOMMON)));
		result.add(new Profession(Identifier.withDefaultNamespace("wandering_trader"),
			Component.translatable("entity.minecraft.wandering_trader"), List.of(), List.of(), wandering));
		return result;
	}

	private static List<ItemStack> jobSites(VillagerProfession profession) {
		Set<Item> items = new LinkedHashSet<>();
		for (Holder.Reference<PoiType> poi : BuiltInRegistries.POINT_OF_INTEREST_TYPE.listElements().toList()) {
			if (profession.heldJobSite().test(poi)) {
				for (BlockState state : poi.value().matchingStates()) {
					Item item = state.getBlock().asItem();
					if (item != net.minecraft.world.item.Items.AIR) {
						items.add(item);
					}
				}
			}
		}
		return items.stream().map(ItemStack::new).toList();
	}

	private static List<Trade> convert(List<VillagerTrade> trades) {
		List<Trade> result = new ArrayList<>();
		for (VillagerTrade trade : trades) {
			try {
				VillagerTradeAccessor acc = (VillagerTradeAccessor) trade;
				boolean[] variable = {false};
				ItemStack wants = cost(acc.luhub$getWants(), variable);
				ItemStack additional = acc.luhub$getAdditionalWants().map(c -> cost(c, variable)).orElse(ItemStack.EMPTY);
				ItemStack gives = acc.luhub$getGives().create();
				result.add(new Trade(wants, additional, gives, variable[0], acc.luhub$getGivenItemModifier().isPresent()));
			} catch (RuntimeException e) {
				LuHub.LOGGER.debug("Handel übersprungen", e);
			}
		}
		return result;
	}

	private static ItemStack cost(TradeCost cost, boolean[] variable) {
		int count = 1;
		if (cost.count().isBound() && cost.count().value() instanceof ConstantValue constant) {
			count = constant.value();
		} else {
			variable[0] = true; // z.B. Verzauberte Bücher: Preis hängt von der Verzauberung ab
		}
		return new ItemStack(cost.item(), Math.max(1, Math.min(count, cost.item().value().getDefaultMaxStackSize())));
	}

	// ------------------------------------------------------------------ Woher die Handel kommen

	private static Function<ResourceKey<TradeSet>, List<VillagerTrade>> resolver(Minecraft mc) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server != null) {
			Registry<TradeSet> sets = server.registryAccess().lookupOrThrow(Registries.TRADE_SET);
			return key -> sets.get(key).map(h -> h.value().trades().stream().filter(Holder::isBound).map(Holder::value).toList()).orElse(List.of());
		}
		Map<Identifier, VillagerTrade> all = vanillaTrades(mc.level.registryAccess());
		return key -> {
			List<VillagerTrade> list = new ArrayList<>();
			for (Identifier id : readTag(key.identifier(), new LinkedHashSet<>())) {
				VillagerTrade trade = all.get(id);
				if (trade != null) {
					list.add(trade);
				}
			}
			return list;
		};
	}

	/** Baut die Vanilla-Handel mit dem Code des Spiels selbst; was der Client nicht kennt (z.B. Strukturen), wird nur als Platzhalter verknüpft. */
	private static Map<Identifier, VillagerTrade> vanillaTrades(RegistryAccess access) {
		Map<Identifier, VillagerTrade> all = new HashMap<>();
		try {
			VillagerTrades.bootstrap(new BootstrapContext<>() {
				@Override
				public Holder.Reference<VillagerTrade> register(ResourceKey<VillagerTrade> key, VillagerTrade value) {
					all.put(key.identifier(), value);
					return Holder.Reference.createStandAlone(stub(), key);
				}

				@Override
				@SuppressWarnings("unchecked")
				public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
					Optional<? extends Registry<?>> registry = access.lookup((ResourceKey) key);
					return registry.isPresent() ? (HolderGetter<S>) registry.get() : stub();
				}

				@Override
				public <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
					return Stream.empty();
				}
			});
		} catch (RuntimeException e) {
			LuHub.LOGGER.warn("Konnte die Dorfbewohner-Handel nicht aufbauen", e);
		}
		return all;
	}

	private static <S> HolderGetter<S> stub() {
		return new HolderGetter<>() {
			@Override
			public Optional<Holder.Reference<S>> get(ResourceKey<S> id) {
				return Optional.of(Holder.Reference.createStandAlone(this, id));
			}

			@Override
			public Optional<HolderSet.Named<S>> get(TagKey<S> id) {
				return Optional.of(HolderSet.emptyNamed(this, id));
			}
		};
	}

	/** Liest einen Handels-Tag (data/&lt;ns&gt;/tags/villager_trade/...) aus dem Spiel und löst verschachtelte Tags auf. */
	private static List<Identifier> readTag(Identifier tag, Set<Identifier> visited) {
		List<Identifier> result = new ArrayList<>();
		if (!visited.add(tag)) {
			return result;
		}
		String path = "data/" + tag.getNamespace() + "/tags/villager_trade/" + tag.getPath() + ".json";
		try (InputStream in = MinecraftServer.class.getClassLoader().getResourceAsStream(path)) {
			if (in == null) {
				return result;
			}
			JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement element : json.getAsJsonArray("values")) {
				String value = element.isJsonObject() ? element.getAsJsonObject().get("id").getAsString() : element.getAsString();
				if (value.startsWith("#")) {
					result.addAll(readTag(Identifier.parse(value.substring(1)), visited));
				} else {
					result.add(Identifier.parse(value));
				}
			}
		} catch (Exception e) {
			LuHub.LOGGER.debug("Handels-Tag {} nicht lesbar", tag, e);
		}
		return result;
	}
}
