package dev.lunaos.luhub.sort;

import dev.lunaos.luhub.LuHub;
import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/** Sortier-Knöpfe in Kisten, Shulkerkisten, Trichtern, Werfern und im Inventar – plus Sortier-Taste. */
public final class SortButtons {
	private SortButtons() {
	}

	public static boolean active() {
		return LuConfig.get().sortButtons && !ExternalMods.SORTED;
	}

	private static boolean hasContainer(Screen screen) {
		return screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen
			|| screen instanceof HopperScreen || screen instanceof DispenserScreen;
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)) {
				return;
			}
			boolean withContainer = hasContainer(screen);
			if (!withContainer && !(screen instanceof InventoryScreen)) {
				return;
			}
			List<SortButton> top = new ArrayList<>();
			List<SortButton> bottom = new ArrayList<>();
			if (withContainer) {
				top.add(new SortButton(SortButton.Icon.SORT, Component.translatable("luhub.sort.container"), b -> Sorter.sort(Sorter.Region.CONTAINER)));
				top.add(new SortButton(SortButton.Icon.RESTOCK, Component.translatable("luhub.sort.restock"), b -> Sorter.restock()));
				top.add(new SortButton(SortButton.Icon.QUICK_STACK, Component.translatable("luhub.sort.quick_stack"), b -> Sorter.quickStack()));
				bottom.add(new SortButton(SortButton.Icon.SORT, Component.translatable("luhub.sort.inventory"), b -> Sorter.sort(Sorter.Region.INVENTORY)));
			} else {
				top.add(new SortButton(SortButton.Icon.SORT, Component.translatable("luhub.sort.inventory"), b -> Sorter.sort(Sorter.Region.INVENTORY)));
			}
			List<SortButton> all = new ArrayList<>(top);
			all.addAll(bottom);
			for (SortButton button : all) {
				net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(button);
			}
			// Positionen jedes Bild neu setzen (das Rezeptbuch verschiebt das Fenster)
			ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				boolean visible = active();
				AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) container;
				int right = acc.luhub$getLeftPos() + acc.luhub$getImageWidth() - 7;
				LuConfig c = LuConfig.get();
				if (c.themeEnabled && c.moonDecoration) {
					right = ThemeDecor.moonX(container) - 3; // links neben dem Mond
				}
				layout(top, right, acc.luhub$getTopPos() + 4, visible);
				layout(bottom, acc.luhub$getLeftPos() + acc.luhub$getImageWidth() - 7,
					acc.luhub$getTopPos() + acc.luhub$getInventoryLabelY() - 2, visible);
			});
			ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> {
				if (!active() || !LuHub.keySort.matches(event)) {
					return true;
				}
				Slot hovered = ((AbstractContainerScreenAccessor) container).luhub$getHoveredSlot();
				Minecraft mc = Minecraft.getInstance();
				if (hovered != null && mc.player != null) {
					Sorter.Region region = Sorter.regionOf(hovered, mc.player);
					if (region == Sorter.Region.INVENTORY || withContainer) {
						Sorter.sort(region);
						return false;
					}
				}
				return true;
			});
		});
	}

	private static void layout(List<SortButton> buttons, int right, int y, boolean visible) {
		int x = right;
		for (SortButton button : buttons) {
			x -= SortButton.SIZE;
			button.setPosition(x, y);
			button.visible = visible;
			x -= 1;
		}
	}
}
