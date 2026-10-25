package dev.lunaos.luhub.test;

import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.hud.StatusBars;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Zeichnet die HUD-Leisten und den Mond zweimal auf schwarzem Grund: oben die alte, pixelweise Version,
 * genau {@link #SHIFT} GUI-Pixel darunter die neue mit gebackenen Texturen. Der Test vergleicht beides Pixel für Pixel.
 */
final class HudCompareScreen extends Screen {
	static final int SHIFT = 64;
	/** GUI-Elemente der letzten Frames: [neu, alt] für HUD-Leisten und Mond. */
	static final int[] HUD_ELEMENTS = new int[2];
	static final int[] MOON_ELEMENTS = new int[2];
	static final int[] SKY_ELEMENTS = new int[2];
	static final int MOON_Y = 4;

	HudCompareScreen() {
		super(Component.literal("HUD-Vergleich"));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(0, 0, width, height, 0xFF000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		LuConfig c = LuConfig.get();
		// neu: an der normalen Stelle
		GuiElementCounter.elements = 0;
		StatusBars.health(graphics, null, c.hud(HudElementType.HEALTH));
		StatusBars.armor(graphics, null, c.hud(HudElementType.ARMOR));
		StatusBars.food(graphics, null, c.hud(HudElementType.FOOD));
		StatusBars.air(graphics, null, c.hud(HudElementType.AIR));
		StatusBars.xpBar(graphics, null, c.hud(HudElementType.XP_BAR));
		HUD_ELEMENTS[0] = GuiElementCounter.elements;
		// alt: SHIFT Pixel höher
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, -SHIFT);
		GuiElementCounter.elements = 0;
		LegacyStatusBars.health(graphics, null, c.hud(HudElementType.HEALTH));
		LegacyStatusBars.armor(graphics, null, c.hud(HudElementType.ARMOR));
		LegacyStatusBars.food(graphics, null, c.hud(HudElementType.FOOD));
		LegacyStatusBars.air(graphics, null, c.hud(HudElementType.AIR));
		LegacyStatusBars.xpBar(graphics, null, c.hud(HudElementType.XP_BAR));
		HUD_ELEMENTS[1] = GuiElementCounter.elements;
		graphics.pose().popMatrix();
		// Mond: klein, klein mit Hover-Schein und groß (wie im Titelbildschirm)
		GuiElementCounter.elements = 0;
		LegacyMoon.extractMoon(graphics, 6, MOON_Y, ThemeDecor.MOON_SIZE, c.colorMoon, false);
		LegacyMoon.extractMoon(graphics, 24, MOON_Y, ThemeDecor.MOON_SIZE, c.colorMoon, true);
		LegacyMoon.extractMoon(graphics, 44, MOON_Y, 40, c.colorMoon, false);
		MOON_ELEMENTS[1] = GuiElementCounter.elements;
		GuiElementCounter.elements = 0;
		ThemeDecor.extractMoon(graphics, 6, MOON_Y + SHIFT, ThemeDecor.MOON_SIZE, c.colorMoon, false);
		ThemeDecor.extractMoon(graphics, 24, MOON_Y + SHIFT, ThemeDecor.MOON_SIZE, c.colorMoon, true);
		ThemeDecor.extractMoon(graphics, 44, MOON_Y + SHIFT, 40, c.colorMoon, false);
		MOON_ELEMENTS[0] = GuiElementCounter.elements;
		// Sternenhimmel nur zählen (auf 1 Pixel beschnitten, damit er den Vergleich nicht verdeckt)
		graphics.enableScissor(0, 0, 1, 1);
		GuiElementCounter.elements = 0;
		ThemeDecor.extractNightSky(graphics, width, height);
		SKY_ELEMENTS[0] = GuiElementCounter.elements;
		GuiElementCounter.elements = 0;
		LegacyMoon.extractNightSky(graphics, width, height);
		SKY_ELEMENTS[1] = GuiElementCounter.elements;
		graphics.disableScissor();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
