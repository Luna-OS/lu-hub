package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/**
 * Hängt sich über die Fabric-HUD-API in die Vanilla-Elemente ein: jedes Element kann
 * verschoben, skaliert, ausgeblendet und (wo sinnvoll) in einem eigenen Stil gezeichnet werden.
 */
public final class LuHud {
	/** true, solange der HUD-Editor offen ist – dann werden z.B. Luftblasen zur Vorschau immer gezeigt. */
	public static boolean previewMode = false;

	private LuHud() {
	}

	@FunctionalInterface
	private interface CustomRenderer {
		/** @return false, wenn doch das Vanilla-Element gezeichnet werden soll. */
		boolean render(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig config);
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, original -> wrap(HudElementType.HOTBAR, original, null));
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, original -> wrap(HudElementType.ARMOR, original, null));
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> wrap(HudElementType.HEALTH, original, StatusBars::health));
		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, original -> wrap(HudElementType.FOOD, original, StatusBars::food));
		HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, original -> wrap(HudElementType.AIR, original, StatusBars::air));
		HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, original -> wrap(HudElementType.XP_BAR, original, StatusBars::xpBar));
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, original -> wrap(HudElementType.XP_LEVEL, original, StatusBars::xpLevel));
	}

	private static HudElement wrap(HudElementType type, HudElement original, CustomRenderer custom) {
		return (graphics, delta) -> {
			LuConfig config = LuConfig.get();
			if (!config.hudEnabled) {
				original.extractRenderState(graphics, delta);
				return;
			}
			HudElementConfig ec = config.hud(type);
			if (!ec.visible && !previewMode) {
				return;
			}
			int baseX = type.defaultX(graphics.guiWidth());
			int baseY = type.defaultY(graphics.guiHeight());
			boolean transformed = ec.dx != 0 || ec.dy != 0 || ec.scale != 1f;
			Matrix3x2fStack pose = graphics.pose();
			if (transformed) {
				pose.pushMatrix();
				pose.translate(baseX + ec.dx, baseY + ec.dy);
				pose.scale(ec.scale, ec.scale);
				pose.translate(-baseX, -baseY);
			}
			boolean handled = custom != null && type.styleable && ec.style != BarStyle.VANILLA
				&& custom.render(graphics, delta, ec);
			if (!handled) {
				original.extractRenderState(graphics, delta);
			}
			if (transformed) {
				pose.popMatrix();
			}
		};
	}

	/** Linke obere Ecke und Größe des Elements, so wie es gerade auf dem Bildschirm liegt. */
	public static int[] bounds(HudElementType type, int screenWidth, int screenHeight) {
		HudElementConfig ec = LuConfig.get().hud(type);
		int x = type.defaultX(screenWidth) + ec.dx;
		int y = type.defaultY(screenHeight) + ec.dy;
		return new int[]{x, y, Math.round(type.width * ec.scale), Math.round(type.height * ec.scale)};
	}
}
