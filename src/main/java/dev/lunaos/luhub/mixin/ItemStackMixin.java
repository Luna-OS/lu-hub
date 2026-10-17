package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.tooltip.LuTooltips;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Hängt Essens-Infos und Shulker-Vorschau als Tooltip-Bild an. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void luhub$extraTooltip(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (cir.getReturnValue().isEmpty()) {
			Optional<TooltipComponent> extra = LuTooltips.extraImage((ItemStack) (Object) this);
			if (extra.isPresent()) {
				cir.setReturnValue(extra);
			}
		}
	}
}
