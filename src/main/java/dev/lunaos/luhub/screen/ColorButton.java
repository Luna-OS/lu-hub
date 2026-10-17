package dev.lunaos.luhub.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Knopf mit Farbvorschau; ein Klick öffnet den Farbwähler. */
public final class ColorButton extends Button.Plain {
	private final Component label;
	private final IntSupplier getter;

	public ColorButton(int x, int y, int width, Component label, IntSupplier getter, IntConsumer setter, Screen parent) {
		super(x, y, width, 20, Component.empty(),
			button -> Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(parent, label, getter.getAsInt(), setter)),
			DEFAULT_NARRATION);
		this.label = label;
		this.getter = getter;
		updateMessage();
	}

	private void updateMessage() {
		setMessage(Component.empty().append(label).append(": ").append(hex(getter.getAsInt())));
	}

	public static String hex(int argb) {
		int alpha = argb >>> 24;
		return alpha == 0xFF
			? String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF)
			: String.format(Locale.ROOT, "#%08X", argb);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		updateMessage();
		super.extractContents(graphics, mouseX, mouseY, a);
		int size = getHeight() - 8;
		int x = getX() + getWidth() - size - 4;
		int y = getY() + 4;
		ColorPickerScreen.extractChecker(graphics, x, y, size, size);
		graphics.fill(x, y, x + size, y + size, getter.getAsInt());
		graphics.outline(x - 1, y - 1, size + 2, size + 2, 0xFF000000);
	}
}
