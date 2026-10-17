package dev.lunaos.luhub.mixin;

import dev.lunaos.luhub.axo.AxoRenderer;
import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ersetzt das grüne Rezeptbuch im Inventar durch Axo. Der Knopf funktioniert weiterhin ganz normal. */
@Mixin(ImageButton.class)
public abstract class ImageButtonMixin {
	@Shadow
	@Final
	protected WidgetSprites sprites;

	@Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
	private void luhub$drawAxo(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		LuConfig config = LuConfig.get();
		if (!config.axoEnabled || this.sprites != RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
			return;
		}
		Screen screen = Minecraft.getInstance().gui.screen();
		if (!(screen instanceof InventoryScreen) && !config.axoEverywhere) {
			return;
		}
		ImageButton self = (ImageButton) (Object) this;
		AxoRenderer.extractOnButton(graphics, self.getX(), self.getY(), self.isHoveredOrFocused(), self.isActive());
		ci.cancel();
	}
}
