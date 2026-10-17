package dev.lunaos.luhub.tooltip;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

/** Inhalts-Vorschau einer Shulkerkiste: 9er-Raster im Theme-Stil, in der Farbe der Kiste. */
public final class ClientShulkerTooltip implements ClientTooltipComponent {
	private static final int SLOT = 18;
	private static final int COLUMNS = 9;
	private static final int PADDING = 3;
	private final ShulkerTooltip data;

	public ClientShulkerTooltip(ShulkerTooltip data) {
		this.data = data;
	}

	private int rows() {
		return Math.max(1, (data.items().size() + COLUMNS - 1) / COLUMNS);
	}

	@Override
	public int getHeight(Font font) {
		return rows() * SLOT + PADDING * 2 + 2;
	}

	@Override
	public int getWidth(Font font) {
		return COLUMNS * SLOT + PADDING * 2;
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		LuConfig c = LuConfig.get();
		NightTheme.Palette palette = data.color() != null && c.themeEnabled && c.shulkerColors
			? NightTheme.Palette.tinted(data.color())
			: NightTheme.Palette.fromConfig();
		int width = getWidth(font);
		int height = rows() * SLOT + PADDING * 2;
		graphics.fill(x, y, x + width, y + height, palette.border());
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, palette.background());
		graphics.fill(x + 1, y + 1, x + width - 1, y + 2, palette.highlight());
		graphics.fill(x + 1, y + 1, x + 2, y + height - 1, palette.highlight());
		// ein paar Sterne
		for (int i = 0; i < 12; i++) {
			int sx = x + 2 + (int) (NightTheme.hash(i, 1, 31) * (width - 4));
			int sy = y + 2 + (int) (NightTheme.hash(i, 2, 31) * (height - 4));
			graphics.fill(sx, sy, sx + 1, sy + 1, i % 3 == 0 ? palette.star() : NightTheme.lerp(palette.background(), palette.highlight(), 0.4f));
		}
		for (int i = 0; i < data.items().size(); i++) {
			int sx = x + PADDING + (i % COLUMNS) * SLOT;
			int sy = y + PADDING + (i / COLUMNS) * SLOT;
			graphics.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, palette.shadow());
			graphics.fill(sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, palette.slot());
			ItemStack stack = data.items().get(i);
			if (!stack.isEmpty()) {
				graphics.item(stack, sx + 1, sy + 1);
				graphics.itemDecorations(font, stack, sx + 1, sy + 1);
			}
		}
	}
}
