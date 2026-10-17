package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.lunaos.luhub.theme.ShulkerTheme;
import dev.lunaos.luhub.theme.ThemedLabelScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shulkerkisten-GUI in der Farbe der Shulkerkiste (im Nachthimmel-Stil). */
@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerBoxScreenMixin implements ThemedLabelScreen {
	@Unique
	private Integer luhub$color;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void luhub$rememberColor(ShulkerBoxMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
		this.luhub$color = ShulkerTheme.colorFor(title);
	}

	@WrapOperation(
		method = "extractBackground",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V")
	)
	private void luhub$tintedBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
										int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
		Identifier tinted = ShulkerTheme.enabled(this.luhub$color) ? ShulkerTheme.texture(texture, this.luhub$color) : null;
		original.call(graphics, pipeline, tinted != null ? tinted : texture, x, y, u, v, width, height, textureWidth, textureHeight);
	}

	@Override
	public int luhub$labelColor() {
		return ShulkerTheme.enabled(this.luhub$color) ? ShulkerTheme.labelColor(this.luhub$color) : 0;
	}
}
