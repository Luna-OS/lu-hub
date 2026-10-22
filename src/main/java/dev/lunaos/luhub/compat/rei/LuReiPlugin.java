package dev.lunaos.luhub.compat.rei;

import dev.lunaos.luhub.screen.WindowDrag;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.DisplayBoundsProvider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * REI richtet seine Leisten an den Grenzen des Fensters aus. Wird ein Fenster mit Lu Hub verschoben,
 * bekommt REI hier weiterhin die ursprüngliche (mittige) Position – die REI-Leisten bleiben also stehen.
 */
public final class LuReiPlugin implements REIClientPlugin {
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
