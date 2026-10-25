package dev.lunaos.luhub.tooltip;

import dev.lunaos.luhub.hud.PixelIcon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;

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

	private static int hungerPixel(char ch, boolean emptyHalf) {
		return switch (ch) {
			case '.' -> 0;
			case 'o' -> 0xFF2A1A06;
			case 'w' -> 0xFFF4F0FF;
			default -> emptyHalf ? 0xFF3A2C1A : (ch == 'h' ? 0xFFFFE6B0 : 0xFFFFC46B);
		};
	}

	private static int saturationPixel(char ch, boolean emptyHalf) {
		return switch (ch) {
			case '.' -> 0;
			case 'o' -> emptyHalf ? 0xFF5A4A20 : 0xFFFFD94A;
			default -> 0x40FFD94A;
		};
	}

	private String saturationText() {
		return String.format(Locale.ROOT, "+%.1f", data.saturation());
	}

	@Override
	public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
		int hunger = data.nutrition();
		Identifier hungerFull = PixelIcon.FOOD.fixed("tooltip_hunger_full", (ch, px, py) -> hungerPixel(ch, false));
		Identifier hungerHalf = PixelIcon.FOOD.fixed("tooltip_hunger_half", (ch, px, py) -> hungerPixel(ch, px < 4));
		for (int i = 0; i < hungerIcons(); i++) {
			PixelIcon.FOOD.blit(graphics, hunger - i * 2 == 1 ? hungerHalf : hungerFull, x + i * 8, y);
		}
		Identifier saturationFull = PixelIcon.FOOD.fixed("tooltip_saturation_full", (ch, px, py) -> saturationPixel(ch, false));
		Identifier saturationHalf = PixelIcon.FOOD.fixed("tooltip_saturation_half", (ch, px, py) -> saturationPixel(ch, px < 4));
		float saturation = data.saturation();
		for (int i = 0; i < saturationIcons(); i++) {
			boolean half = saturation - i * 2 < 1.5f && saturation - i * 2 > 0;
			PixelIcon.FOOD.blit(graphics, half ? saturationHalf : saturationFull, x + i * 8, y + 11);
		}
		int textX = x + Math.max(hungerIcons(), saturationIcons()) * 8 + 4;
		graphics.text(font, hungerText(), textX, y + 1, 0xFFFFC46B, true);
		graphics.text(font, saturationText(), textX, y + 12, 0xFFFFD94A, true);
	}
}
