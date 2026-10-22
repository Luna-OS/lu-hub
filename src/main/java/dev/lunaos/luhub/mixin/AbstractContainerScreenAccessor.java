package dev.lunaos.luhub.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int luhub$getLeftPos();

	@Accessor("topPos")
	int luhub$getTopPos();

	@Accessor("leftPos")
	void luhub$setLeftPos(int leftPos);

	@Accessor("topPos")
	void luhub$setTopPos(int topPos);

	@Accessor("titleLabelY")
	int luhub$getTitleLabelY();

	@Accessor("imageWidth")
	int luhub$getImageWidth();

	@Accessor("imageHeight")
	int luhub$getImageHeight();

	@Accessor("inventoryLabelY")
	int luhub$getInventoryLabelY();

	@Accessor("hoveredSlot")
	Slot luhub$getHoveredSlot();
}
