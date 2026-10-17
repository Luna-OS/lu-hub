package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.config.Anchor;
import dev.lunaos.luhub.config.DebugPanelConfig;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.debug.LuDebugOverlay;
import dev.lunaos.luhub.debug.LuDebugOverlay.PanelRect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F3-Editor: jedes Panel der F3-Ansicht per Drag &amp; Drop verschieben, an der Ecke größer/kleiner ziehen,
 * per Rechtsklick ausblenden und mit C die Farbe ändern.
 */
public final class DebugEditorScreen extends Screen {
	private static final int HANDLE = 5;

	private final Screen parent;
	private final List<PanelRect> rects = new ArrayList<>();
	private Map<Identifier, List<String>> lines;
	private Identifier selected;
	private Identifier dragging;
	private boolean resizing;
	private double grabX;
	private double grabY;

	public DebugEditorScreen(Screen parent) {
		super(Component.translatable("luhub.debug_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		LuDebugOverlay.editorOpen = true;
		lines = LuDebugOverlay.previewLines();
		int y = height - 24;
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 154, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("luhub.config.reset_debug_layout"), b -> LuConfig.resetDebugLayout())
			.bounds(width / 2 - 50, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("luhub.config.title_short"), b -> {
			LuConfig.save();
			minecraft.gui.setScreen(new LuConfigScreen(this));
		}).bounds(width / 2 + 54, y, 100, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.fill(0, 0, width, height, 0x50050716);
		graphics.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);
		graphics.fill(0, height / 2, width, height / 2 + 1, 0x20FFFFFF);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		rects.clear();
		LuDebugOverlay.extract(graphics, lines, rects, selected != null ? selected : dragging);
		PanelRect hovered = rectAt(mouseX, mouseY);
		for (PanelRect r : rects) {
			boolean hot = r == hovered || r.id().equals(selected);
			graphics.fill(r.x() + r.width() - HANDLE, r.y() + r.height() - HANDLE, r.x() + r.width(), r.y() + r.height(),
				hot ? 0xFFFFFFFF : 0x90B9C0FF);
		}
		if (hovered != null) {
			DebugPanelConfig pc = LuConfig.get().panel(hovered.id().toString());
			String info = LuDebugOverlay.title(hovered.id()) + String.format(Locale.ROOT, "  %.0f%%", pc.scale * 100)
				+ (pc.hidden ? " " + Component.translatable("luhub.hud_editor.hidden").getString() : "");
			graphics.setTooltipForNextFrame(font, Component.literal(info), mouseX, mouseY);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		HudEditorScreen.extractHelp(graphics, font, Component.translatable("luhub.debug_editor.help"), width, height - 36, true);
	}

	private PanelRect rectAt(double mx, double my) {
		for (int i = rects.size() - 1; i >= 0; i--) {
			if (rects.get(i).contains(mx, my)) {
				return rects.get(i);
			}
		}
		return null;
	}

	private PanelRect rectOf(Identifier id) {
		for (PanelRect r : rects) {
			if (r.id().equals(id)) {
				return r;
			}
		}
		return null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		PanelRect r = rectAt(event.x(), event.y());
		if (r == null) {
			selected = null;
			return false;
		}
		selected = r.id();
		DebugPanelConfig pc = LuConfig.get().panel(r.id().toString());
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			pc.hidden = !pc.hidden;
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = r.id();
			resizing = event.x() >= r.x() + r.width() - HANDLE - 1 && event.y() >= r.y() + r.height() - HANDLE - 1;
			grabX = event.x() - r.x();
			grabY = event.y() - r.y();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) {
			return super.mouseDragged(event, dx, dy);
		}
		PanelRect r = rectOf(dragging);
		if (r == null) {
			return true;
		}
		LuConfig config = LuConfig.get();
		DebugPanelConfig pc = config.panel(dragging.toString());
		if (resizing) {
			float unscaledW = r.width() / r.scale();
			float unscaledH = r.height() / r.scale();
			float total = Math.max((float) (event.x() - r.x()) / unscaledW, (float) (event.y() - r.y()) / unscaledH);
			pc.scale = Math.clamp(Math.round(total / config.debugScale * 20f) / 20f, 0.25f, 4f);
			// Position fixieren, damit das Panel beim Größe-Ziehen nicht springt
			place(pc, r.x(), r.y(), r.width(), r.height());
		} else {
			int x = (int) Math.round(event.x() - grabX);
			int y = (int) Math.round(event.y() - grabY);
			x = Math.clamp(x, 0, Math.max(0, width - r.width()));
			y = Math.clamp(y, 0, Math.max(0, height - r.height()));
			place(pc, x, y, r.width(), r.height());
		}
		return true;
	}

	/** Speichert eine Panel-Position relativ zum nächstgelegenen Bildschirm-Anker. */
	private void place(DebugPanelConfig pc, int x, int y, int w, int h) {
		Anchor anchor = Anchor.nearest(x + w / 2f, y + h / 2f, width, height);
		pc.placed = true;
		pc.anchor = anchor;
		pc.x = x + Math.round(anchor.pivotX() * w) - anchor.baseX(width);
		pc.y = y + Math.round(anchor.pivotY() * h) - anchor.baseY(height);
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
		PanelRect r = rectAt(x, y);
		if (r == null) {
			return super.mouseScrolled(x, y, scrollX, scrollY);
		}
		DebugPanelConfig pc = LuConfig.get().panel(r.id().toString());
		pc.scale = Math.clamp(Math.round((pc.scale + (float) Math.signum(scrollY) * 0.05f) * 20f) / 20f, 0.25f, 4f);
		selected = r.id();
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (selected != null) {
			DebugPanelConfig pc = LuConfig.get().panel(selected.toString());
			PanelRect r = rectOf(selected);
			int step = minecraft.hasShiftDown() ? 10 : 1;
			switch (event.key()) {
				case InputConstants.KEY_C -> {
					Identifier id = selected;
					minecraft.gui.setScreen(new ColorPickerScreen(this, Component.literal(LuDebugOverlay.title(id)),
						LuDebugOverlay.accentColor(id, pc), v -> pc.color = v));
				}
				case InputConstants.KEY_R -> {
					pc.placed = false;
					pc.scale = 1f;
					pc.color = 0;
				}
				case InputConstants.KEY_DELETE, InputConstants.KEY_BACKSPACE -> pc.hidden = !pc.hidden;
				case InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT, InputConstants.KEY_UP, InputConstants.KEY_DOWN -> {
					if (r != null) {
						int dx = event.key() == InputConstants.KEY_LEFT ? -step : event.key() == InputConstants.KEY_RIGHT ? step : 0;
						int dy = event.key() == InputConstants.KEY_UP ? -step : event.key() == InputConstants.KEY_DOWN ? step : 0;
						place(pc, r.x() + dx, r.y() + dy, r.width(), r.height());
					}
				}
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
		LuDebugOverlay.editorOpen = false;
		LuConfig.save();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
