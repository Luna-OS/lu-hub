package dev.lunaos.luhub;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.axo.AxoRenderer;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.hud.LuHud;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.mixin.ImageButtonAccessor;
import dev.lunaos.luhub.screen.DebugEditorScreen;
import dev.lunaos.luhub.screen.HudEditorScreen;
import dev.lunaos.luhub.screen.LuConfigScreen;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LuHub implements ClientModInitializer {
	public static final String MOD_ID = "luhub";
	public static final Logger LOGGER = LoggerFactory.getLogger("Lu Hub");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
	public static KeyMapping keySettings;
	public static KeyMapping keyHudEditor;
	public static KeyMapping keyDebugEditor;
	public static KeyMapping keyDebugToggle;

	@Override
	public void onInitializeClient() {
		LuConfig.load();
		LuHud.register();

		keySettings = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.luhub.settings", InputConstants.Type.KEYBOARD, InputConstants.KEY_U, CATEGORY));
		keyHudEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.luhub.hud_editor", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY));
		keyDebugEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.luhub.debug_editor", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));
		keyDebugToggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.luhub.debug_toggle", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(LuHub::handleKeys);
		registerContainerDecor();
		LOGGER.info("Lu Hub geladen – gute Nacht, Axo!");
	}

	private static void handleKeys(Minecraft mc) {
		while (keySettings.consumeClick()) {
			mc.gui.setScreen(new LuConfigScreen(null));
		}
		while (keyHudEditor.consumeClick()) {
			mc.gui.setScreen(new HudEditorScreen(null));
		}
		while (keyDebugEditor.consumeClick()) {
			mc.gui.setScreen(new DebugEditorScreen(null));
		}
		while (keyDebugToggle.consumeClick()) {
			mc.debugEntries.toggleDebugOverlay();
		}
	}

	private static boolean hasRecipeButton(Screen screen) {
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget instanceof ImageButtonAccessor button && button.luhub$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
				return true;
			}
		}
		return false;
	}

	/** Mond oben rechts in jedem Container – ein Klick darauf öffnet die Lu-Hub-Einstellungen. */
	private static void registerContainerDecor() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)) {
				return;
			}
			ScreenEvents.afterBackground(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				LuConfig c = LuConfig.get();
				if (c.themeEnabled && c.moonDecoration) {
					boolean hovered = ThemeDecor.isOverMoon(container, mouseX, mouseY);
					ThemeDecor.extractMoon(graphics, ThemeDecor.moonX(container), ThemeDecor.moonY(container), ThemeDecor.MOON_SIZE, c.colorMoon, hovered);
				}
			});
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				LuConfig c = LuConfig.get();
				if (c.axoEnabled && container instanceof InventoryScreen && !hasRecipeButton(container)) {
					// REI (oder eine andere Mod) hat das Rezeptbuch ausgeblendet – Axo bleibt trotzdem an seinem Platz
					AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) container;
					int ax = acc.luhub$getLeftPos() + 104;
					int ay = acc.luhub$getTopPos() + acc.luhub$getImageHeight() / 2 - 22;
					boolean hovered = mouseX >= ax && mouseX < ax + 20 && mouseY >= ay && mouseY < ay + 18;
					AxoRenderer.extractOnButton(graphics, ax, ay, hovered, true);
				}
				if (c.themeEnabled && c.moonDecoration && ThemeDecor.isOverMoon(container, mouseX, mouseY)
					&& container.getMenu().getCarried().isEmpty()) {
					graphics.setTooltipForNextFrame(client.font, Component.translatable("luhub.moon.tooltip"), mouseX, mouseY);
				}
			});
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				LuConfig c = LuConfig.get();
				if (c.themeEnabled && c.moonDecoration && event.button() == InputConstants.MOUSE_BUTTON_LEFT
					&& container.getMenu().getCarried().isEmpty() && ThemeDecor.isOverMoon(container, event.x(), event.y())) {
					client.gui.setScreen(new LuConfigScreen(container));
					return false;
				}
				return true;
			});
		});
	}
}
