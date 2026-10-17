package dev.lunaos.luhub.tooltip;

import dev.lunaos.luhub.hud.PixelIcon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.Locale;

/** Zwei Zeilen Keulen: oben Hunger (gefüllt), unten Sättigung (goldener Rand) – mit Zahl dahinter. */
public final class ClientFoodTooltip implements ClientTooltipComponent {
	private static final int MAX_ICONS = 10;
	private final FoodTooltip data;

	public ClientFoodTooltip(FoodTooltip data) {
		this.data = data;
	}

	private int hungerIcons() {
		return Math.min(MAX_ICONS, (data.nutrition() + 1) / 2);
	}

	private int saturationIcons() {
		return Math.min(MAX_ICONS, Math.max(data.saturation() > 0 ? 1 : 0, Math.round(data.saturation() / 2f)));
	}

	@Override
	public int getHeight(Font font) {
		return 22;
	}

	@Override
	public int getWidth(Font font) {
		int icons = Math.max(hungerIcons(), saturationIcons()) * 8 + 2;
		return icons + 4 + Math.max(font.width(hungerText()), font.width(saturationText()));
	}

	private String hungerText() {
		return "+" + data.nutrition();
	}

	private String saturationText() {
		return String.format(Locale.ROOT, "+%.1f", data.saturation());
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		int hunger = data.nutrition();
		for (int i = 0; i < hungerIcons(); i++) {
			boolean half = hunger - i * 2 == 1;
			int ix = x + i * 8;
			PixelIcon.FOOD.draw(graphics, ix, y, (ch, px, py) -> switch (ch) {
				case '.' -> 0;
				case 'o' -> 0xFF2A1A06;
				case 'w' -> 0xFFF4F0FF;
				default -> half && px < 4 ? 0xFF3A2C1A : (ch == 'h' ? 0xFFFFE6B0 : 0xFFFFC46B);
			});
		}
		float saturation = data.saturation();
		for (int i = 0; i < saturationIcons(); i++) {
			boolean half = saturation - i * 2 < 1.5f && saturation - i * 2 > 0;
			int ix = x + i * 8;
			PixelIcon.FOOD.draw(graphics, ix, y + 11, (ch, px, py) -> switch (ch) {
				case '.' -> 0;
				case 'o' -> half && px < 4 ? 0xFF5A4A20 : 0xFFFFD94A;
				default -> 0x40FFD94A;
			});
		}
		int textX = x + Math.max(hungerIcons(), saturationIcons()) * 8 + 4;
		graphics.text(font, hungerText(), textX, y + 1, 0xFFFFC46B, true);
		graphics.text(font, saturationText(), textX, y + 12, 0xFFFFD94A, true);
	}
}
