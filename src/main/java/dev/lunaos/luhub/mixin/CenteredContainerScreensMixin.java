package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.lunaos.luhub.screen.WindowDrag;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Diese Fenster rechnen ihre Position (Hintergrund, Klickflächen, Knöpfe, Axo) selbst aus der Bildschirmmitte aus,
 * z.B. {@code (this.width - this.imageWidth) / 2}, statt leftPos/topPos zu nehmen. In diesen Klassen wird
 * {@code width}/{@code height} ausschließlich dafür gelesen – hier liefern sie Werte, mit denen die Rechnung genau
 * beim verschobenen Fenster landet. So wandert das ganze Fenster mit, nicht nur Items und Text.
 */
@Mixin({
	AbstractFurnaceScreen.class, AbstractMountInventoryScreen.class, AnvilScreen.class, BeaconScreen.class,
	BrewingStandScreen.class, ContainerScreen.class, CrafterScreen.class, CraftingScreen.class, DispenserScreen.class,
	EnchantmentScreen.class, GrindstoneScreen.class, HopperScreen.class, InventoryScreen.class, MerchantScreen.class,
	ShulkerBoxScreen.class
})
public abstract class CenteredContainerScreensMixin {
	@ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "width:I", opcode = Opcodes.GETFIELD), require = 0)
	private int luhub$width(int width) {
		return width + 2 * WindowDrag.shiftX((AbstractContainerScreen<?>) (Object) this);
	}

	@ModifyExpressionValue(method = "*", at = @At(value = "FIELD", target = "height:I", opcode = Opcodes.GETFIELD), require = 0)
	private int luhub$height(int height) {
		return height + 2 * WindowDrag.shiftY((AbstractContainerScreen<?>) (Object) this);
	}
}
