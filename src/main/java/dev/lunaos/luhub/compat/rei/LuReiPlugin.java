package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.screen.WindowDrag;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.Items;
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * REI-Erweiterungen von Lu Hub: Erz-Höhen, Dorfbewohner-Berufe und -Handel.
 * <p>
 * Außerdem: REI richtet seine Leisten an den Grenzen des Fensters aus. Wird ein Fenster mit Lu Hub verschoben,
 * bekommt REI hier weiterhin die ursprüngliche (mittige) Position – die REI-Leisten bleiben also stehen.
 */
public final class LuReiPlugin implements REIClientPlugin {
	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.add(new OreCategory(), new ProfessionCategory(), new TradeCategory());
		registry.addWorkstations(OreDisplay.ID, EntryStacks.of(Items.IRON_PICKAXE), EntryStacks.of(Items.DIAMOND_PICKAXE), EntryStacks.of(Items.NETHERITE_PICKAXE));
		registry.addWorkstations(ProfessionDisplay.ID, EntryStacks.of(Items.VILLAGER_SPAWN_EGG));
		registry.addWorkstations(TradeDisplay.ID, EntryStacks.of(Items.VILLAGER_SPAWN_EGG), EntryStacks.of(Items.WANDERING_TRADER_SPAWN_EGG));
	}

	@Override
	public void registerDisplays(DisplayRegistry registry) {
		registry.add(new OreDisplay(null, OreData.Dim.OVERWORLD));
		registry.add(new OreDisplay(null, OreData.Dim.NETHER));
		for (OreData ore : OreData.values()) {
			registry.add(new OreDisplay(ore, ore.dim));
		}
		for (VillagerTradeData.Profession profession : VillagerTradeData.load()) {
			if (!profession.jobSites().isEmpty()) {
				registry.add(new ProfessionDisplay(profession));
			}
			profession.trades().forEach((level, trades) -> {
				for (VillagerTradeData.Trade trade : trades) {
					registry.add(new TradeDisplay(profession, level, trade));
				}
			});
		}
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerDecider(new DisplayBoundsProvider<AbstractContainerScreen<?>>() {
			@Override
			public Rectangle getScreenBounds(AbstractContainerScreen<?> screen) {
				int[] bounds = WindowDrag.unmovedBounds(screen);
				return bounds == null ? null : new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);
			}

			@Override
			public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
				return AbstractContainerScreen.class.isAssignableFrom(screen);
			}

			@Override
			public double getPriority() {
				// vor REIs eigener Container-Regel (-10)
				return 10.0;
			}
		});
	}
}
