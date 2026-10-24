package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.config.TooltipInfoConfig;
import dev.lunaos.luhub.tooltip.TooltipInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Tooltip-Editor: Zusatz-Infos per Drag &amp; Drop sortieren, an-/ausschalten und einfärben –
 * mit Live-Vorschau an Beispiel-Items (oder dem Item in der Hand).
 */
public final class TooltipEditorScreen extends Screen {
	private static final int ROW = 18;
	private static final int LIST_WIDTH = 196;
	private static final int TOP = 32;

	private final Screen parent;
	private final List<ItemStack> samples = new ArrayList<>();
	private static int sampleIndex;
	private int scroll;
	private int dragging = -1;
	private double dragGrabY;
	private double dragMouseY;

	public TooltipEditorScreen(Screen parent) {
		super(Component.translatable("luhub.tooltip_editor.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		buildSamples();
		int x1 = previewX();
		addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleSample(-1)).bounds(x1, TOP, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleSample(1)).bounds(x1 + LIST_WIDTH - 20, TOP, 20, 20).build());

		LuConfig c = LuConfig.get();
		int y = height - 50;
		int w = 130;
		int left = width / 2 - w * 3 / 2 - 4;
		addRenderableWidget(CycleButton.onOffBuilder(c.tooltipInfos)
			.create(left, y, w, 20, Component.translatable("luhub.config.tooltip_infos"), (b, v) -> c.tooltipInfos = v));
		addRenderableWidget(CycleButton.onOffBuilder(c.tooltipInfosShift)
			.create(left + w + 4, y, w, 20, Component.translatable("luhub.config.tooltip_shift"), (b, v) -> c.tooltipInfosShift = v));
		addRenderableWidget(CycleButton.onOffBuilder(c.tooltipHideAttributes)
			.create(left + 2 * (w + 4), y, w, 20, Component.translatable("luhub.config.tooltip_hide_attributes"), (b, v) -> c.tooltipHideAttributes = v));
		addRenderableWidget(Button.builder(Component.translatable("luhub.config.reset"), b -> LuConfig.resetTooltips())
			.bounds(width / 2 - 154, height - 26, 150, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 + 4, height - 26, 150, 20).build());
	}

	private void buildSamples() {
		samples.clear();
		if (minecraft.player != null && !minecraft.player.getMainHandItem().isEmpty()) {
			samples.add(minecraft.player.getMainHandItem().copy());
		}
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.set(DataComponents.DAMAGE, 1100);
		pickaxe.set(DataComponents.REPAIR_COST, 3);
		samples.add(pickaxe);
		samples.add(new ItemStack(Items.COAL, 23));
		samples.add(new ItemStack(Items.GOLDEN_CARROT, 12));
		samples.add(new ItemStack(Items.WHEAT_SEEDS, 40));
		samples.add(new ItemStack(Items.GLOWSTONE, 8));
		samples.add(new ItemStack(Items.BLAZE_POWDER, 5));
		samples.add(new ItemStack(Items.ENDER_PEARL, 7));
		ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
		sword.set(DataComponents.DAMAGE, 1500);
		samples.add(sword);
		sampleIndex = Math.floorMod(sampleIndex, samples.size());
	}

	private void cycleSample(int step) {
		sampleIndex = Math.floorMod(sampleIndex + step, samples.size());
	}

	private int listX() {
		return width / 2 - LIST_WIDTH - 6;
	}

	private int previewX() {
		return width / 2 + 6;
	}

	private Component help() {
		return Component.translatable("luhub.tooltip_editor.help");
	}

	/** Unterkante der Liste: über dem (je nach Breite ein- oder zweizeiligen) Hilfetext. */
	private int listBottom() {
		int helpLines = font.split(help(), width - 20).size();
		return height - 62 - helpLines * 10 - 4;
	}

	private int maxScroll() {
		return Math.max(0, LuConfig.get().tooltipLines.size() * ROW - (listBottom() - TOP));
	}

	private int rowY(int index) {
		return TOP + index * ROW - scroll;
	}

	private int rowAt(double mx, double my) {
		if (mx < listX() || mx >= listX() + LIST_WIDTH || my < TOP || my >= listBottom()) {
			return -1;
		}
		int index = (int) ((my - TOP + scroll) / ROW);
		return index < LuConfig.get().tooltipLines.size() ? index : -1;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		LuConfig c = LuConfig.get();
		List<TooltipInfoConfig> lines = c.tooltipLines;
		int x = listX();
		graphics.fill(x - 2, TOP - 2, x + LIST_WIDTH + 2, listBottom() + 2, 0x80050716);
		graphics.outline(x - 3, TOP - 3, LIST_WIDTH + 6, listBottom() - TOP + 6, 0xFF6B74C8);
		graphics.enableScissor(x - 2, TOP - 2, x + LIST_WIDTH + 2, listBottom() + 2);
		int hovered = dragging < 0 ? rowAt(mouseX, mouseY) : -1;
		for (int i = 0; i < lines.size(); i++) {
			if (i == dragging) {
				continue;
			}
			extractRow(graphics, lines.get(i), x, rowY(i), i == hovered, mouseX, mouseY);
		}
		graphics.disableScissor();
		if (maxScroll() > 0) {
			// Scrollleiste
			int trackHeight = listBottom() - TOP;
			int thumb = Math.max(12, trackHeight * trackHeight / (lines.size() * ROW));
			int thumbY = TOP + (trackHeight - thumb) * scroll / maxScroll();
			graphics.fill(x + LIST_WIDTH + 4, TOP, x + LIST_WIDTH + 6, listBottom(), 0x40B9C0FF);
			graphics.fill(x + LIST_WIDTH + 4, thumbY, x + LIST_WIDTH + 6, thumbY + thumb, 0xFFB9C0FF);
		}
		if (dragging >= 0 && dragging < lines.size()) {
			int y = (int) Math.round(dragMouseY - dragGrabY);
			graphics.fill(x - 2, y - 1, x + LIST_WIDTH + 2, y + ROW - 1, 0xC0241C4A);
			extractRow(graphics, lines.get(dragging), x, y, true, mouseX, mouseY);
		}

		// Vorschau
		int px = previewX();
		ItemStack sample = samples.get(sampleIndex);
		graphics.centeredText(font, Component.translatable("luhub.tooltip_editor.preview"), px + LIST_WIDTH / 2, TOP + 6, 0xFFB9C0FF);
		graphics.item(sample, px + 24, TOP + 2);
		graphics.setTooltipForNextFrame(font, sample, px - 10, TOP + 46);
		HudEditorScreen.extractHelp(graphics, font, help(), width, height - 62, true);
	}

	private void extractRow(GuiGraphicsExtractor graphics, TooltipInfoConfig line, int x, int y, boolean hot, int mouseX, int mouseY) {
		TooltipInfo info = TooltipInfo.byId(line.id);
		if (info == null) {
			return;
		}
		if (hot) {
			graphics.fill(x - 1, y - 1, x + LIST_WIDTH + 1, y + ROW - 1, 0x40B9C0FF);
		}
		// Griff
		for (int i = 0; i < 3; i++) {
			graphics.fill(x + 2, y + 4 + i * 3, x + 9, y + 5 + i * 3, 0xFF8A93D8);
		}
		// Kästchen
		int bx = x + 13;
		int by = y + 3;
		graphics.outline(bx, by, 10, 10, 0xFFB9C0FF);
		if (line.enabled) {
			graphics.fill(bx + 2, by + 2, bx + 8, by + 8, 0xFFF4A9D8);
		}
		int color = line.color != 0 ? line.color : 0xFF7CF08A;
		graphics.text(font, info.label(), x + 28, y + 4, line.enabled ? color : 0xFF6B6F8F);
		// Farbfeld
		int sx = x + LIST_WIDTH - 16;
		if (line.color == 0) {
			graphics.fillGradient(sx, by, sx + 12, by + 10, 0xFF7CF08A, 0xFFFF5A5A);
		} else {
			graphics.fill(sx, by, sx + 12, by + 10, line.color);
		}
		graphics.outline(sx - 1, by - 1, 14, 12, 0xFF000000);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int index = rowAt(event.x(), event.y());
		if (index < 0) {
			return false;
		}
		TooltipInfoConfig line = LuConfig.get().tooltipLines.get(index);
		TooltipInfo info = TooltipInfo.byId(line.id);
		int x = listX();
		double localX = event.x() - x;
		if (localX >= LIST_WIDTH - 18 && info != null) {
			if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
				line.color = info.defaultColor;
			} else {
				int current = line.color != 0 ? line.color : 0xFF7CF08A;
				minecraft.gui.setScreen(new ColorPickerScreen(this, info.label(), current, v -> line.color = v));
			}
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT || (localX >= 11 && localX < 26)) {
			line.enabled = !line.enabled;
			LuConfig.save();
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			dragging = index;
			dragGrabY = event.y() - rowY(index);
			dragMouseY = event.y();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging < 0) {
			return super.mouseDragged(event, dx, dy);
		}
		dragMouseY = event.y();
		List<TooltipInfoConfig> lines = LuConfig.get().tooltipLines;
		double center = event.y() - dragGrabY + ROW / 2.0;
		int target = Math.clamp((int) ((center - TOP + scroll) / ROW), 0, lines.size() - 1);
		if (target != dragging) {
			lines.add(target, lines.remove(dragging));
			dragging = target;
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging >= 0) {
			dragging = -1;
			LuConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (x >= previewX()) {
			cycleSample(scrollY > 0 ? -1 : 1);
			return true;
		}
		scroll = Math.clamp(scroll - (int) Math.signum(scrollY) * ROW, 0, maxScroll());
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		LuConfig.save();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
