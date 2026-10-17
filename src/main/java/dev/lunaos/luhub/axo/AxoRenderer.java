package dev.lunaos.luhub.axo;

import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Matrix3x2fStack;

/**
 * Zeichnet Axo, das Maskottchen, überall dort, wo sonst das grüne Rezeptbuch ist.
 * <p>
 * Sprite-Sheet assets/luhub/textures/gui/axo.png: 5 Bilder à 28x19 Pixel
 * (0/1 = Ruhe mit wackelnden Kiemen, 2 = Blinzeln, 3/4 = fröhlich mit Schwanzwedeln beim Drüberfahren).
 * Erzeugt von tools/axo_sprite.py; per Resource-Pack ersetzbar.
 */
public final class AxoRenderer {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("luhub", "textures/gui/axo.png");
	public static final int FRAME_WIDTH = 28;
	public static final int FRAME_HEIGHT = 19;
	private static final int FRAMES = 5;

	private AxoRenderer() {
	}

	private static final Identifier BOOK_BUTTON = Identifier.withDefaultNamespace("recipe_book/button");
	private static final Identifier BOOK_BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("recipe_book/button_highlighted");
	private static final Identifier BOOK_TOAST = Identifier.withDefaultNamespace("toast/recipe_book");

	/**
	 * Ersetzt die Sprites des grünen Rezeptbuchs (Knopf in Inventar, Werkbank, Öfen ... und das Tutorial-Toast) durch Axo.
	 *
	 * @return true, wenn Axo gezeichnet wurde und das Original-Sprite entfallen soll
	 */
	public static boolean replaceBookSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
		boolean button = sprite.equals(BOOK_BUTTON);
		boolean hovered = sprite.equals(BOOK_BUTTON_HIGHLIGHTED);
		if (!button && !hovered && !sprite.equals(BOOK_TOAST)) {
			return false;
		}
		LuConfig config = LuConfig.get();
		if (!config.axoEnabled) {
			return false;
		}
		if (config.axoOnlyInventory && !(Minecraft.getInstance().gui.screen() instanceof InventoryScreen)) {
			return false;
		}
		extract(graphics, x + (width - FRAME_WIDTH) / 2, y + (height - FRAME_HEIGHT) / 2, hovered, true);
		return true;
	}

	/** Zeichnet Axo mittig über einem 20x18 großen Knopf an (buttonX, buttonY). */
	public static void extractOnButton(GuiGraphicsExtractor graphics, int buttonX, int buttonY, boolean hovered, boolean active) {
		extract(graphics, buttonX + (20 - FRAME_WIDTH) / 2, buttonY + (18 - FRAME_HEIGHT) / 2, hovered, active);
	}

	public static void extract(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, boolean active) {
		boolean animated = LuConfig.get().axoAnimated;
		long millis = animated ? Util.getMillis() : 0L;
		double time = millis / 1000.0;

		int frame;
		float bob;
		if (hovered && animated) {
			frame = (millis / 220) % 2 == 0 ? 3 : 4;
			// kleiner Hüpfer
			bob = -Math.abs((float) Math.sin(time * 7.0)) * 2.0f;
		} else {
			boolean blinking = millis % 4200 < 140;
			frame = blinking ? 2 : (millis / 650) % 2 == 0 ? 0 : 1;
			bob = (float) Math.sin(time * 2.0) * 0.75f;
		}

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(0, Math.round(bob));
		int tint = active ? -1 : ARGB.color(160, 0xFFFFFF);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, frame * FRAME_WIDTH, 0f,
			FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH * FRAMES, FRAME_HEIGHT, tint);
		pose.popMatrix();

		if (hovered && animated) {
			extractHearts(graphics, x, y, time);
		}
	}

	/** Kleine Herzchen, die über Axo aufsteigen, wenn man mit der Maus drüberfährt. */
	private static void extractHearts(GuiGraphicsExtractor graphics, int x, int y, double time) {
		for (int i = 0; i < 2; i++) {
			double t = (time * 0.9 + i * 0.5) % 1.0;
			int hx = x + 8 + i * 10 + (int) Math.round(Math.sin(time * 3 + i) * 1.5);
			int hy = y - (int) (t * 8);
			int alpha = (int) (255 * (1.0 - t));
			int color = ARGB.color(alpha, NightTheme.lerp(0xFFFF8FC8, 0xFFFFC2E2, (float) t) & 0xFFFFFF);
			graphics.fill(hx, hy, hx + 1, hy + 1, color);
			graphics.fill(hx + 2, hy, hx + 3, hy + 1, color);
			graphics.fill(hx, hy + 1, hx + 3, hy + 2, color);
			graphics.fill(hx + 1, hy + 2, hx + 2, hy + 3, color);
		}
	}
}
