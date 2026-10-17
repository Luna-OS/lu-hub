package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.hud.LuHud;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * HUD-Editor: Elemente per Drag &amp; Drop verschieben, an der Ecke größer/kleiner ziehen,
 * mit dem Mausrad skalieren und per Rechtsklick aus- oder einblenden.
 */
public final class HudEditorScreen extends Screen {
	private static final int HANDLE = 5;
	private static final int SNAP = 4;

	private final Screen parent;
	private HudElementType selected;
	private HudElementType dragging;
	private boolean resizing;
	private double grabX;
	private double grabY;

	public HudEditorScreen(Screen parent) {
		super(Component.translatable("luhub.hud_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		LuHud.previewMode = true;
		int y = 4;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 154, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("luhub.config.reset_hud_layout"), b -> LuConfig.resetHudLayout())
			.bounds(width / 2 - 50, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("luhub.config.title_short"), b -> {
			LuConfig.save();
			minecraft.gui.setScreen(new LuConfigScreen(this));
		}).bounds(width / 2 + 54, y, 100, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		// kein Abdunkeln – die echte HUD soll sichtbar bleiben
		graphics.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);
		graphics.fill(0, height / 2, width, height / 2 + 1, 0x20FFFFFF);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		LuConfig config = LuConfig.get();
		HudElementType hovered = elementAt(mouseX, mouseY);
		for (HudElementType type : HudElementType.values()) {
			int[] b = LuHud.bounds(type, width, height);
			HudElementConfig ec = config.hud(type);
			boolean active = type == selected || type == dragging;
			int border = !ec.visible ? 0xC0FF6B6B : active ? 0xFFFFFFFF : type == hovered ? 0xFFB9C0FF : 0x90969FEB;
			graphics.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], active ? 0x30B9C0FF : 0x18FFFFFF);
			graphics.outline(b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, border);
			// Griff zum Größe-Ziehen
			int hx = b[0] + b[2] - HANDLE + 1;
			int hy = b[1] + b[3] - HANDLE + 1;
			graphics.fill(hx, hy, hx + HANDLE, hy + HANDLE, active || type == hovered ? 0xFFFFFFFF : 0xB0B9C0FF);
			if (active || type == hovered) {
				Component label = Component.empty().append(type.label())
					.append(String.format(java.util.Locale.ROOT, " %.0f%%", ec.scale * 100))
					.append(ec.visible ? Component.empty() : Component.translatable("luhub.hud_editor.hidden"));
				int ly = b[1] - 11 < 26 ? b[1] + b[3] + 2 : b[1] - 11;
				graphics.text(font, label, b[0], ly, 0xFFFFFFFF);
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		extractHelp(graphics, font, Component.translatable("luhub.hud_editor.help"), width, 28, false);
	}

	/** Zentrierter, umgebrochener Hilfetext; wächst bei {@code fromBottom} nach oben. */
	static void extractHelp(GuiGraphicsExtractor graphics, Font font, Component text, int width, int y, boolean fromBottom) {
		List<FormattedCharSequence> lines = font.split(text, width - 20);
		int startY = fromBottom ? y - (lines.size() - 1) * 10 : y;
		for (int i = 0; i < lines.size(); i++) {
			graphics.centeredText(font, lines.get(i), width / 2, startY + i * 10, 0xFFD9DDFF);
		}
	}

	private HudElementType elementAt(double mx, double my) {
		HudElementType[] types = HudElementType.values();
		HudElementType best = null;
		int bestArea = Integer.MAX_VALUE;
		for (HudElementType type : types) {
			int[] b = LuHud.bounds(type, width, height);
			if (mx >= b[0] - 1 && my >= b[1] - 1 && mx <= b[0] + b[2] + 1 && my <= b[1] + b[3] + 1) {
				int area = b[2] * b[3];
				if (area < bestArea) { // kleinstes Element gewinnt, damit überlappende greifbar bleiben
					best = type;
					bestArea = area;
				}
			}
		}
		return best;
	}

	private static boolean onHandle(int[] b, double mx, double my) {
		return mx >= b[0] + b[2] - HANDLE - 1 && my >= b[1] + b[3] - HANDLE - 1 && mx <= b[0] + b[2] + 2 && my <= b[1] + b[3] + 2;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		HudElementType type = elementAt(event.x(), event.y());
		if (type == null) {
			selected = null;
			return false;
		}
		selected = type;
		HudElementConfig ec = LuConfig.get().hud(type);
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			ec.visible = !ec.visible;
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			int[] b = LuHud.bounds(type, width, height);
			dragging = type;
			resizing = onHandle(b, event.x(), event.y());
			grabX = event.x() - b[0];
			grabY = event.y() - b[1];
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) {
			return super.mouseDragged(event, dx, dy);
		}
		HudElementConfig ec = LuConfig.get().hud(dragging);
		int baseX = dragging.defaultX(width);
		int baseY = dragging.defaultY(height);
		if (resizing) {
			int[] b = LuHud.bounds(dragging, width, height);
			float sx = (float) (event.x() - b[0]) / dragging.width;
			float sy = (float) (event.y() - b[1]) / dragging.height;
			float scale = Math.max(sx, sy);
			ec.scale = Math.clamp(Math.round(scale * 20f) / 20f, 0.25f, 4f);
		} else {
			int newX = (int) Math.round(event.x() - grabX);
			int newY = (int) Math.round(event.y() - grabY);
			if (!minecraft.hasShiftDown()) {
				// am Bildschirmrand und an der Mitte einrasten
				int w = Math.round(dragging.width * ec.scale);
				int h = Math.round(dragging.height * ec.scale);
				newX = snap(newX, w, width);
				newY = snap(newY, h, height);
			}
			ec.dx = newX - baseX;
			ec.dy = newY - baseY;
		}
		return true;
	}

	private static int snap(int pos, int size, int screen) {
		if (Math.abs(pos) <= SNAP) {
			return 0;
		}
		if (Math.abs(pos + size - screen) <= SNAP) {
			return screen - size;
		}
		int center = screen / 2 - size / 2;
		if (Math.abs(pos - center) <= SNAP) {
			return center;
		}
		return pos;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			resizing = false;
			LuConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		HudElementType type = elementAt(x, y);
		if (type == null) {
			return super.mouseScrolled(x, y, scrollX, scrollY);
		}
		HudElementConfig ec = LuConfig.get().hud(type);
		ec.scale = Math.clamp(Math.round((ec.scale + (float) Math.signum(scrollY) * 0.05f) * 20f) / 20f, 0.25f, 4f);
		selected = type;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (selected != null) {
			HudElementConfig ec = LuConfig.get().hud(selected);
			int step = minecraft.hasShiftDown() ? 10 : 1;
			switch (event.key()) {
				case InputConstants.KEY_LEFT -> ec.dx -= step;
				case InputConstants.KEY_RIGHT -> ec.dx += step;
				case InputConstants.KEY_UP -> ec.dy -= step;
				case InputConstants.KEY_DOWN -> ec.dy += step;
				case InputConstants.KEY_R -> {
					ec.dx = 0;
					ec.dy = 0;
					ec.scale = 1f;
				}
				case InputConstants.KEY_DELETE, InputConstants.KEY_BACKSPACE -> ec.visible = !ec.visible;
				default -> {
					return super.keyPressed(event);
				}
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		LuHud.previewMode = false;
		LuConfig.save();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
