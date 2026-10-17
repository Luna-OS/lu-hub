package dev.lunaos.luhub.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Daten für die Hunger/Sättigungs-Zeile im Tooltip. */
public record FoodTooltip(int nutrition, float saturation) implements TooltipComponent {
}
