package dev.lunaos.luhub.tooltip;

import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.food.FoodInfo;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;
import java.util.Optional;

/**
 * Eingebaute Tooltip-Erweiterungen: Hunger/Sättigung bei Essen (wie AppleSkin)
 * und eine Inhalts-Vorschau bei Shulkerkisten (wie Shulker Box Tooltip) – beides im Theme-Stil.
 */
public final class LuTooltips {
	private LuTooltips() {
	}

	public static void register() {
		ClientTooltipComponentCallback.EVENT.register(data -> switch (data) {
			case FoodTooltip food -> new ClientFoodTooltip(food);
			case ShulkerTooltip shulker -> new ClientShulkerTooltip(shulker);
			default -> null;
		});
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (shulkerPreviewFor(stack).isPresent() && LuConfig.get().shulkerPreviewHideLines) {
				// die Vanilla-Zeilen "Diamant x12", "und 3 weitere ..." ersetzt die Vorschau
				lines.removeIf(LuTooltips::isContainerLine);
			}
		});
	}

	private static boolean isContainerLine(Component line) {
		return line.getContents() instanceof TranslatableContents t
			&& (t.getKey().equals("item.container.item_count") || t.getKey().equals("item.container.more_items"));
	}

	/** Wird von ItemStack.getTooltipImage aufgerufen, wenn das Item selbst kein Tooltip-Bild hat. */
	public static Optional<TooltipComponent> extraImage(ItemStack stack) {
		Optional<TooltipComponent> shulker = shulkerPreviewFor(stack);
		if (shulker.isPresent()) {
			return shulker;
		}
		LuConfig c = LuConfig.get();
		if (c.foodTooltip && FoodInfo.active()) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null && (food.nutrition() > 0 || food.saturation() > 0)) {
				return Optional.of(new FoodTooltip(food.nutrition(), food.saturation()));
			}
		}
		return Optional.empty();
	}

	private static Optional<TooltipComponent> shulkerPreviewFor(ItemStack stack) {
		LuConfig c = LuConfig.get();
		if (!c.shulkerPreview || ExternalMods.SHULKER_BOX_TOOLTIP) {
			return Optional.empty();
		}
		if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock shulker)) {
			return Optional.empty();
		}
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents == null || contents.nonEmptyItemCopyStream().findAny().isEmpty()) {
			return Optional.empty();
		}
		List<ItemStack> items = contents.itemCopies().toList();
		DyeColor color = shulker.getColor();
		return Optional.of(new ShulkerTooltip(items, color == null ? null : color.getTextureDiffuseColor() & 0xFFFFFF));
	}
}
