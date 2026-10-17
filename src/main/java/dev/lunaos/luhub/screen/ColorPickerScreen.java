package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

/** Farbwähler mit Farbfeld, Farbton- und Deckkraft-Leiste, Hex-Eingabe und Vorlagen. */
public final class ColorPickerScreen extends Screen {
	private static final int SQUARE = 100;
	private static final int BAR = 12;
	private static final int CELL = 4;
	private static final int[] PRESETS = {
		0xFF262E5C, 0xFF181D42, 0xFF07081A, 0xFF969FEB, 0xFFB9C0FF, 0xFFD9DDFF, 0xFFFFF1B8, 0xFFFFF4C2,
		0xFFFF9BD2, 0xFFE86AA8, 0xFFC2367F, 0xFFFFC46B, 0xFF8FE3FF, 0xFF9DF5C4, 0xFFB49BFF, 0xFFFFFFFF,
		0xFFE40303, 0xFFFF8C00, 0xFFFFED00, 0xFF008026, 0xFF004DFF, 0xFF750787, 0xFF5BCEFA, 0xFFF5A9B8,
		0xFFD60270, 0xFF9B4F96, 0xFF0038A8, 0xFFFF218C, 0xFFFFD800, 0xFF21B1FF, 0xFF000000, 0xA00B0E24
	};

	private final Screen parent;
	private final int original;
	private final IntConsumer setter;
	private float hue;
	private float saturation;
	private float brightness;
	private float alpha;
	private EditBox hexBox;
	private int dragging = -1; // 0 Feld, 1 Farbton, 2 Deckkraft
	private boolean updatingHex;

	public ColorPickerScreen(Screen parent, Component title, int initial, IntConsumer setter) {
		super(title);
		this.parent = parent;
		this.original = initial;
		this.setter = setter;
		setFromArgb(initial);
	}

	private int left() {
		return width / 2 - (SQUARE + BAR * 2 + 16 + 90) / 2;
	}

	private int top() {
		return Math.max(30, height / 2 - 75);
	}

	@Override
	protected void init() {
		int left = left();
		int top = top();
		int infoX = left + SQUARE + BAR * 2 + 16 + 4;
		hexBox = new EditBox(font, infoX, top + 52, 86, 18, Component.literal("Hex"));
		hexBox.setMaxLength(9);
		hexBox.setValue(ColorButton.hex(current()));
		hexBox.setResponder(this::onHexTyped);
		addRenderableWidget(hexBox);

		int buttonsY = top + SQUARE + 46;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
			setter.accept(current());
			onClose();
		}).bounds(width / 2 - 102, buttonsY, 100, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> {
			setter.accept(original);
			onClose();
		}).bounds(width / 2 + 2, buttonsY, 100, 20).build());
	}

	private void onHexTyped(String text) {
		if (updatingHex) {
			return;
		}
		String s = text.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		try {
			if (s.length() == 6) {
				setFromArgb(0xFF000000 | Integer.parseUnsignedInt(s, 16));
			} else if (s.length() == 8) {
				setFromArgb((int) Long.parseLong(s, 16));
			}
		} catch (NumberFormatException ignored) {
			// unvollständige Eingabe – einfach weiter tippen lassen
		}
	}

	private void syncHex() {
		updatingHex = true;
		hexBox.setValue(ColorButton.hex(current()));
		updatingHex = false;
	}

	private int current() {
		return ARGB.color(Math.round(alpha * 255), hsvToRgb(hue, saturation, brightness));
	}

	private void setFromArgb(int argb) {
		alpha = ARGB.alpha(argb) / 255f;
		float r = ARGB.red(argb) / 255f;
		float g = ARGB.green(argb) / 255f;
		float b = ARGB.blue(argb) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		brightness = max;
		saturation = max == 0 ? 0 : delta / max;
		if (delta > 0) {
			float h;
			if (max == r) {
				h = ((g - b) / delta) % 6f;
			} else if (max == g) {
				h = (b - r) / delta + 2f;
			} else {
				h = (r - g) / delta + 4f;
			}
			hue = ((h / 6f) % 1f + 1f) % 1f;
		}
	}

	static int hsvToRgb(float h, float s, float v) {
		return Mth.hsvToRgb(Mth.clamp(h, 0f, 0.9999f), Mth.clamp(s, 0f, 1f), Mth.clamp(v, 0f, 1f)) & 0xFFFFFF;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int left = left();
		int top = top();
		graphics.centeredText(font, title, width / 2, top - 18, 0xFFFFFFFF);

		// Sättigung/Helligkeit-Feld
		for (int cy = 0; cy < SQUARE; cy += CELL) {
			for (int cx = 0; cx < SQUARE; cx += CELL) {
				float s = cx / (float) (SQUARE - CELL);
				float v = 1f - cy / (float) (SQUARE - CELL);
				graphics.fill(left + cx, top + cy, left + cx + CELL, top + cy + CELL, 0xFF000000 | hsvToRgb(hue, s, v));
			}
		}
		graphics.outline(left - 1, top - 1, SQUARE + 2, SQUARE + 2, 0xFF000000);
		int markerX = left + Math.round(saturation * (SQUARE - 1));
		int markerY = top + Math.round((1 - brightness) * (SQUARE - 1));
		graphics.outline(markerX - 2, markerY - 2, 5, 5, 0xFFFFFFFF);

		// Farbton
		int hueX = left + SQUARE + 6;
		for (int i = 0; i < SQUARE; i += 2) {
			graphics.fill(hueX, top + i, hueX + BAR, top + i + 2, 0xFF000000 | hsvToRgb(i / (float) SQUARE, 1f, 1f));
		}
		graphics.outline(hueX - 1, top - 1, BAR + 2, SQUARE + 2, 0xFF000000);
		int hy = top + Math.round(hue * (SQUARE - 1));
		graphics.fill(hueX - 2, hy, hueX + BAR + 2, hy + 1, 0xFFFFFFFF);

		// Deckkraft
		int alphaX = hueX + BAR + 6;
		extractChecker(graphics, alphaX, top, BAR, SQUARE);
		int rgb = hsvToRgb(hue, saturation, brightness);
		graphics.fillGradient(alphaX, top, alphaX + BAR, top + SQUARE, 0xFF000000 | rgb, rgb);
		graphics.outline(alphaX - 1, top - 1, BAR + 2, SQUARE + 2, 0xFF000000);
		int ay = top + Math.round((1 - alpha) * (SQUARE - 1));
		graphics.fill(alphaX - 2, ay, alphaX + BAR + 2, ay + 1, 0xFFFFFFFF);

		// Vorschau alt/neu
		int infoX = alphaX + BAR + 8;
		graphics.text(font, Component.translatable("luhub.color.new"), infoX, top, 0xFFFFFFFF);
		extractChecker(graphics, infoX, top + 10, 40, 16);
		graphics.fill(infoX, top + 10, infoX + 40, top + 26, current());
		graphics.text(font, Component.translatable("luhub.color.old"), infoX + 46, top, 0xFFAAAAAA);
		extractChecker(graphics, infoX + 46, top + 10, 40, 16);
		graphics.fill(infoX + 46, top + 10, infoX + 86, top + 26, original);
		graphics.text(font, "Hex:", infoX, top + 41, 0xFFFFFFFF);

		// Vorlagen
		int presetsY = top + SQUARE + 8;
		int presetSize = 10;
		int perRow = 16;
		int presetsX = width / 2 - perRow * (presetSize + 2) / 2;
		for (int i = 0; i < PRESETS.length; i++) {
			int px = presetsX + (i % perRow) * (presetSize + 2);
			int py = presetsY + (i / perRow) * (presetSize + 2);
			extractChecker(graphics, px, py, presetSize, presetSize);
			graphics.fill(px, py, px + presetSize, py + presetSize, PRESETS[i]);
			boolean hover = mouseX >= px && mouseY >= py && mouseX < px + presetSize && mouseY < py + presetSize;
			graphics.outline(px - 1, py - 1, presetSize + 2, presetSize + 2, hover ? 0xFFFFFFFF : 0xFF000000);
		}
	}

	/** Schachbrett hinter halbtransparenten Farben. */
	static void extractChecker(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, 0xFFBFBFBF);
		for (int cy = 0; cy < h; cy += 4) {
			for (int cx = ((cy / 4) % 2) * 4; cx < w; cx += 8) {
				graphics.fill(x + cx, y + cy, Math.min(x + cx + 4, x + w), Math.min(y + cy + 4, y + h), 0xFF7F7F7F);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int left = left();
			int top = top();
			double mx = event.x();
			double my = event.y();
			int hueX = left + SQUARE + 6;
			int alphaX = hueX + BAR + 6;
			if (my >= top && my < top + SQUARE) {
				if (mx >= left && mx < left + SQUARE) {
					dragging = 0;
				} else if (mx >= hueX && mx < hueX + BAR) {
					dragging = 1;
				} else if (mx >= alphaX && mx < alphaX + BAR) {
					dragging = 2;
				}
			}
			if (dragging >= 0) {
				updateDrag(mx, my);
				return true;
			}
			int presetsY = top + SQUARE + 8;
			int presetsX = width / 2 - 16 * 12 / 2;
			for (int i = 0; i < PRESETS.length; i++) {
				int px = presetsX + (i % 16) * 12;
				int py = presetsY + (i / 16) * 12;
				if (mx >= px && my >= py && mx < px + 10 && my < py + 10) {
					setFromArgb(PRESETS[i]);
					syncHex();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging >= 0) {
			updateDrag(event.x(), event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = -1;
		return super.mouseReleased(event);
	}

	private void updateDrag(double mx, double my) {
		int left = left();
		int top = top();
		float fy = Mth.clamp((float) (my - top) / (SQUARE - 1), 0f, 1f);
		switch (dragging) {
			case 0 -> {
				saturation = Mth.clamp((float) (mx - left) / (SQUARE - 1), 0f, 1f);
				brightness = 1f - fy;
			}
			case 1 -> hue = fy;
			case 2 -> alpha = 1f - fy;
			default -> {
				return;
			}
		}
		syncHex();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
