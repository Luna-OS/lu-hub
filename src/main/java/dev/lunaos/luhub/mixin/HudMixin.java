package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hinterlegt den ausgewählten Hotbar-Slot heller – nach dem Auswahlrahmen, aber vor den Items. */
@Mixin(Hud.class)
public abstract class HudMixin {
	@Inject(
		method = "extractItemHotbar",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 1, shift = At.Shift.AFTER)
	)
	private void luhub$highlightSelectedSlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		LuConfig c = LuConfig.get();
		if (!c.hotbarHighlight || !(Minecraft.getInstance().getCameraEntity() instanceof Player player)) {
			return;
		}
		int x = graphics.guiWidth() / 2 - 91 + player.getInventory().getSelectedSlot() * 20 + 2;
		int y = graphics.guiHeight() - 20;
		graphics.fill(x, y, x + 18, y + 18, c.colorSelectedSlot);
	}
}
