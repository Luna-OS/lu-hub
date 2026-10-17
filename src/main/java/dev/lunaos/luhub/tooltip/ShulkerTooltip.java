package dev.lunaos.luhub.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Inhalt einer Shulkerkiste für die Tooltip-Vorschau; color = RGB der Kiste oder null (ungefärbt). */
public record ShulkerTooltip(List<ItemStack> items, Integer color) implements TooltipComponent {
}
