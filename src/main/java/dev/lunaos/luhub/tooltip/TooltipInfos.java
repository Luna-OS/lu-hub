package dev.lunaos.luhub.tooltip;

import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.config.TooltipInfoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BrewingFuel;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nützliche Zusatz-Zeilen im Item-Tooltip (Haltbarkeit, Anzahl im Inventar, Brennstoff, Kompost, Reparatur ...).
 * Welche Zeilen erscheinen, in welcher Reihenfolge und Farbe, wird im Tooltip-Editor eingestellt.
 */
public final class TooltipInfos {
	/** Brenndauer in Ticks der Vanilla-Brennstoffe (in 26.3 datengetrieben und nicht zum Client synchronisiert). */
	private static final Map<String, Integer> COOKING_TICKS = Map.ofEntries(
		Map.entry("cooking/time_bamboo", 50), Map.entry("cooking/time_wool_slabs", 50), Map.entry("cooking/time_wool_carpets", 67),
		Map.entry("cooking/time_dry_plants", 100), Map.entry("cooking/time_wood_items_extra_small", 100), Map.entry("cooking/time_wool", 100),
		Map.entry("cooking/time_wood_slabs", 150), Map.entry("cooking/time_wood_items_large", 200), Map.entry("cooking/time_roots", 300),
		Map.entry("cooking/time_wood_blocks", 300), Map.entry("cooking/time_wood_items_small", 300), Map.entry("cooking/time_hanging_signs", 800),
		Map.entry("cooking/time_boats", 1200), Map.entry("cooking/time_coal", 1600), Map.entry("cooking/time_blaze_rod", 2400),
		Map.entry("cooking/time_dried_kelp_block", 4001), Map.entry("cooking/time_coal_block", 16000), Map.entry("cooking/time_lava_bucket", 20000)
	);
	/** Wahrscheinlichkeit (Prozent), dass ein Item den Komposter um eine Stufe füllt. */
	private static final Map<String, Integer> COMPOST_CHANCE = Map.of(
		"compostable/low", 30, "compostable/low_medium", 50, "compostable/medium", 65,
		"compostable/medium_high", 85, "compostable/always_add_one", 100
	);
	private static final Map<String, Integer> BREWING_USES = Map.of("brewing/uses_default", 20);

	private TooltipInfos() {
	}

	/** Aus dem ItemTooltipCallback: Vanilla-Zeilen anpassen und die Zusatz-Infos anhängen. */
	public static void append(ItemStack stack, TooltipFlag flag, List<Component> lines) {
		LuConfig c = LuConfig.get();
		if (stack.isEmpty()) {
			return;
		}
		if (c.tooltipHideAttributes) {
			removeAttributeLines(lines);
		}
		if (!c.tooltipInfos) {
			return;
		}
		List<Component> extra = new ArrayList<>();
		for (TooltipInfoConfig lc : c.tooltipLines) {
			TooltipInfo info = TooltipInfo.byId(lc.id);
			if (info == null || !lc.enabled) {
				continue;
			}
			Component line = build(info, stack, flag, lc.color);
			if (line != null) {
				extra.add(line);
			}
		}
		if (extra.isEmpty()) {
			return;
		}
		if (c.tooltipInfosShift && !Minecraft.getInstance().hasShiftDown()) {
			lines.add(Component.translatable("luhub.tooltip.shift_hint").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		lines.addAll(extra);
	}

	private static MutableComponent colored(MutableComponent line, int color) {
		return line.withColor(color & 0xFFFFFF);
	}

	static Component build(TooltipInfo info, ItemStack stack, TooltipFlag flag, int color) {
		int col = color != 0 ? color : info.defaultColor;
		return switch (info) {
			case DURABILITY -> {
				if (!stack.isDamageableItem()) {
					yield null;
				}
				int max = stack.getMaxDamage();
				int left = max - stack.getDamageValue();
				float fraction = max == 0 ? 1f : left / (float) max;
				int auto = ARGB.srgbLerp(fraction, 0xFFFF5A5A, 0xFF7CF08A);
				yield colored(Component.translatable("luhub.tooltip.durability", left, max, Math.round(fraction * 100)), color != 0 ? color : auto);
			}
			case INVENTORY_COUNT -> inventoryCount(stack, col);
			case REPAIR_ITEM -> {
				Repairable repairable = stack.get(DataComponents.REPAIRABLE);
				if (repairable == null || repairable.items().size() == 0) {
					yield null;
				}
				Holder<Item> first = repairable.items().get(0);
				MutableComponent name = new ItemStack(first).getHoverName().copy();
				if (repairable.items().size() > 1) {
					name.append(" +" + (repairable.items().size() - 1));
				}
				yield colored(Component.translatable("luhub.tooltip.repair_item", name), col);
			}
			case REPAIR_COST -> {
				int cost = stack.getOrDefault(DataComponents.REPAIR_COST, 0);
				yield cost > 0 ? colored(Component.translatable("luhub.tooltip.repair_cost", cost), col) : null;
			}
			case FUEL -> {
				CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
				if (fuel == null) {
					yield null;
				}
				Integer ticks = resolve(fuel.burnTime(), COOKING_TICKS);
				if (ticks == null) {
					yield colored(Component.translatable("luhub.tooltip.fuel_unknown"), col);
				}
				int total = ticks * stack.getCount();
				MutableComponent line = Component.translatable("luhub.tooltip.fuel", number(ticks / 200.0), number(ticks / 20.0));
				if (stack.getCount() > 1) {
					line.append(Component.translatable("luhub.tooltip.fuel_stack", number(total / 200.0)));
				}
				yield colored(line, col);
			}
			case BREWING_FUEL -> {
				BrewingFuel fuel = stack.get(DataComponents.BREWING_FUEL);
				if (fuel == null) {
					yield null;
				}
				Integer uses = resolve(fuel.uses(), BREWING_USES);
				yield colored(uses == null ? Component.translatable("luhub.tooltip.brewing_fuel_unknown")
					: Component.translatable("luhub.tooltip.brewing_fuel", uses), col);
			}
			case COMPOST -> {
				Compostable compostable = stack.get(DataComponents.COMPOSTABLE);
				if (compostable == null) {
					yield null;
				}
				Integer chance = compostable.layers() instanceof ResolvableInt.Constant constant
					? Math.min(100, constant.value() * 100) : resolve(compostable.layers(), COMPOST_CHANCE);
				yield colored(chance == null ? Component.translatable("luhub.tooltip.compost_unknown")
					: Component.translatable("luhub.tooltip.compost", chance), col);
			}
			case FOOD_VALUES -> {
				FoodProperties food = stack.get(DataComponents.FOOD);
				yield food == null ? null
					: colored(Component.translatable("luhub.tooltip.food", food.nutrition(), number(food.saturation())), col);
			}
			case LIGHT -> {
				Block block = block(stack);
				int light = block == null ? 0 : block.defaultBlockState().getLightEmission();
				yield light > 0 ? colored(Component.translatable("luhub.tooltip.light", light), col) : null;
			}
			case STACK_SIZE -> {
				int max = stack.getMaxStackSize();
				yield max == 1 ? colored(Component.translatable("luhub.tooltip.not_stackable"), col)
					: max != 64 ? colored(Component.translatable("luhub.tooltip.stack_size", max), col) : null;
			}
			case ENCHANTABILITY -> {
				Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
				yield enchantable == null ? null : colored(Component.translatable("luhub.tooltip.enchantability", enchantable.value()), col);
			}
			case BLAST_RESISTANCE -> {
				Block block = block(stack);
				yield block == null ? null
					: colored(Component.translatable("luhub.tooltip.blast_resistance", number(block.getExplosionResistance())), col);
			}
			case MOD_NAME -> {
				if (ExternalMods.REI) {
					yield null; // REI zeigt den Mod-Namen schon an
				}
				String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
				String name = namespace.equals(Identifier.DEFAULT_NAMESPACE) ? "Minecraft"
					: FabricLoader.getInstance().getModContainer(namespace).map(m -> m.getMetadata().getName()).orElse(namespace);
				yield colored(Component.literal(name).withStyle(ChatFormatting.ITALIC), col);
			}
			case ITEM_ID -> flag.isAdvanced() ? null // erweiterte Tooltips (F3+H) zeigen die ID schon
				: colored(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()), col);
		};
	}

	private static Component inventoryCount(ItemStack stack, int color) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		Inventory inventory = mc.player.getInventory();
		int inInventory = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack s = inventory.getItem(i);
			if (ItemStack.isSameItem(s, stack)) {
				inInventory += s.getCount();
			}
		}
		int here = 0;
		if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
			for (Slot slot : screen.getMenu().slots) {
				if (slot.container != inventory && ItemStack.isSameItem(slot.getItem(), stack)) {
					here += slot.getItem().getCount();
				}
			}
		}
		if (inInventory == 0 && here == 0) {
			return null;
		}
		MutableComponent line = Component.translatable("luhub.tooltip.inventory", stacks(inInventory, stack.getMaxStackSize()));
		if (here > 0) {
			line.append("  ").append(Component.translatable("luhub.tooltip.here", stacks(here, stack.getMaxStackSize())));
		}
		return colored(line, color);
	}

	/** "137 (2 × 64 + 9)" – bei weniger als einem Stapel nur die Zahl. */
	private static Component stacks(int count, int stackSize) {
		if (stackSize <= 1 || count < stackSize) {
			return Component.literal(String.valueOf(count));
		}
		int full = count / stackSize;
		int rest = count % stackSize;
		return Component.translatable(rest == 0 ? "luhub.tooltip.stacks_even" : "luhub.tooltip.stacks", count, full, stackSize, rest);
	}

	private static Integer resolve(ResolvableInt value, Map<String, Integer> vanilla) {
		return switch (value) {
			case ResolvableInt.Constant constant -> constant.value();
			case ResolvableInt.Reference reference -> {
				Identifier id = reference.key().identifier();
				yield id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? vanilla.get(id.getPath()) : null;
			}
		};
	}

	private static Block block(ItemStack stack) {
		return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
	}

	private static String number(double value) {
		return Math.abs(value - Math.round(value)) < 0.05 ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
	}

	/** Entfernt "Wenn in der Haupthand:" samt Angriffs-/Rüstungswerten (und die Leerzeile davor). */
	private static void removeAttributeLines(List<Component> lines) {
		for (int i = lines.size() - 1; i >= 0; i--) {
			Component line = lines.get(i);
			if (hasKey(line, "attribute.modifier.")) {
				lines.remove(i);
			} else if (hasKey(line, "item.modifiers.")) {
				lines.remove(i);
				if (i > 0 && lines.get(i - 1).getString().isEmpty()) {
					lines.remove(--i);
				}
			}
		}
	}

	private static boolean hasKey(Component component, String prefix) {
		if (component.getContents() instanceof TranslatableContents t && t.getKey().startsWith(prefix)) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (hasKey(sibling, prefix)) {
				return true;
			}
		}
		return false;
	}
}
