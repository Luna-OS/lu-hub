package dev.lunaos.luhub.sort;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Kleiner Knopf (11x11) mit Pixel-Symbol im Theme-Stil. */
public final class SortButton extends Button.Plain {
	public static final int SIZE = 11;

	public enum Icon {
		SORT(
			"#######",
			".......",
			"#####..",
			".......",
			"###....",
			".......",
			"#......"
		),
		QUICK_STACK(
			"...#...",
			"..###..",
			".#####.",
			"...#...",
			"#..#..#",
			"#.....#",
			"#######"
		),
		RESTOCK(
			"...#...",
			"...#...",
			".#####.",
			"..###..",
			"#..#..#",
			"#.....#",
			"#######"
		);

		final String[] rows;

		Icon(String... rows) {
			this.rows = rows;
		}
	}

	private final Icon icon;

	public SortButton(Icon icon, Component tooltip, OnPress onPress) {
		super(0, 0, SIZE, SIZE, tooltip, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		LuConfig c = LuConfig.get();
		boolean hot = isHoveredOrFocused();
		int x = getX();
		int y = getY();
		graphics.fill(x, y, x + SIZE, y + SIZE, c.colorBorder);
		graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, hot ? NightTheme.lerp(c.colorBackground, c.colorHighlight, 0.35f) : c.colorSlot);
		int iconColor = hot ? 0xFFFFFFFF : c.colorHighlight;
		for (int row = 0; row < icon.rows.length; row++) {
			String line = icon.rows[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					graphics.fill(x + 2 + col, y + 2 + row, x + 3 + col, y + 3 + row, iconColor);
				}
			}
		}
	}
}
