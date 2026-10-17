package dev.lunaos.luhub.compat;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.tooltip.LuTooltips;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Falls "Shulker Box Tooltip" installiert ist: dessen Vorschau-Fenster bekommt den Nachthimmel-Look
 * und die Farbe der Shulkerkiste – genau wie Lu Hubs eigene Vorschau.
 * <p>
 * Die Mod färbt ein graues Fenster-Sprite per Multiplikation mit der Shulkerfarbe. Wir hinterlegen deshalb
 * ein neutrales Sprite (Grautöne + Sterne) und ersetzen die Multiplikationsfarbe so, dass das Ergebnis
 * dem eingefärbten Theme entspricht.
 */
public final class ShulkerBoxTooltipCompat {
	public static final Identifier WINDOW_SPRITE = Identifier.fromNamespaceAndPath("shulkerboxtooltip", "shulker_box_tooltip");
	/** Helligkeit, auf die der Fenster-Hintergrund im neutralen Sprite abgebildet wird. */
	private static final int NEUTRAL_BACKGROUND = 0xBF;
	private static final int DEFAULT_COLOR = 0xFFFFFF;
	private static final int UNDYED_SHULKER = 0x976797;

	private ShulkerBoxTooltipCompat() {
	}

	public static boolean themed() {
		return LuConfig.get().themeEnabled;
	}

	/** Neutrale Palette für das Fenster-Sprite – die Farbe kommt erst beim Zeichnen dazu. */
	public static NightTheme.Palette neutralPalette() {
		int bg = NEUTRAL_BACKGROUND;
		return new NightTheme.Palette(0xFF101010, 0xFF505050, 0xFF6E6E6E, 0xFF000000 | bg << 16 | bg << 8 | bg, 0xFFFFFFFF, 0xFFFFFFFF);
	}

	/** Farbe der gefärbten Shulkerkiste im Slot unter der Maus, sonst null. */
	private static Integer hoveredShulkerColor() {
		if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
			return null;
		}
		Slot slot = ((AbstractContainerScreenAccessor) screen).luhub$getHoveredSlot();
		if (slot == null || !(slot.getItem().getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ShulkerBoxBlock shulker)) {
			return null;
		}
		DyeColor dye = shulker.getColor();
		return dye == null ? null : dye.getTextureDiffuseColor() & 0xFFFFFF;
	}

	/** Neue Multiplikationsfarbe für die von Shulker Box Tooltip übergebene Farbe. */
	public static int tint(int color) {
		LuConfig c = LuConfig.get();
		int rgb = color & 0xFFFFFF;
		if (rgb == DEFAULT_COLOR) {
			// "Farben benutzen" ist in Shulker Box Tooltip aus – Farbe direkt von der Kiste unter der Maus holen
			Integer hovered = LuTooltips.currentShulkerColor();
			if (hovered == null) {
				hovered = hoveredShulkerColor();
			}
			if (hovered != null) {
				rgb = hovered;
			}
		}
		int background = rgb == DEFAULT_COLOR || rgb == UNDYED_SHULKER || !c.shulkerColors
			? c.colorBackground
			: NightTheme.Palette.tinted(rgb).background();
		int r = Math.min(255, ((background >> 16) & 0xFF) * 255 / NEUTRAL_BACKGROUND);
		int g = Math.min(255, ((background >> 8) & 0xFF) * 255 / NEUTRAL_BACKGROUND);
		int b = Math.min(255, (background & 0xFF) * 255 / NEUTRAL_BACKGROUND);
		return (color & 0xFF000000) | r << 16 | g << 8 | b;
	}
}
