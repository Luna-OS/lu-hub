package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.axo.AxoRenderer;
import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Einstellungen von Lu Hub, aufgeteilt in Tabs. */
public final class LuConfigScreen extends Screen {
	private enum Tab { GENERAL, HUD, DEBUG, THEME, AXO, EXTRAS }

	private static final int ROW = 24;
	private static final int COLUMN_WIDTH = 150;
	private static final int CONTENT_TOP = 52;

	private static Tab tab = Tab.GENERAL;
	private static HudElementType selectedHud = HudElementType.HEALTH;

	private final Screen parent;
	private final List<AbstractWidget> content = new ArrayList<>();
	private final List<Integer> contentY = new ArrayList<>();
	private int scroll;
	private int contentHeight;
	private int nextRow;
	private int nextColumn;
	private boolean resourcesDirty;
	private Button f3KeyButton;
	private boolean listeningForF3Key;

	public LuConfigScreen(Screen parent) {
		super(Component.translatable("luhub.config.title"));
		this.parent = parent;
	}

	// ------------------------------------------------------------------ Aufbau

	@Override
	protected void init() {
		content.clear();
		contentY.clear();
		nextRow = 0;
		nextColumn = 0;
		f3KeyButton = null;

		Tab[] tabs = Tab.values();
		int tabWidth = Math.min(80, (width - 20) / tabs.length);
		int tabsLeft = width / 2 - tabWidth * tabs.length / 2;
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			Button b = Button.builder(Component.translatable("luhub.tab." + t.name().toLowerCase()), button -> {
				tab = t;
				scroll = 0;
				rebuildWidgets();
			}).bounds(tabsLeft + i * tabWidth, 22, tabWidth - 2, 20).build();
			b.active = t != tab;
			addRenderableWidget(b);
		}

		switch (tab) {
			case GENERAL -> buildGeneral();
			case HUD -> buildHud();
			case DEBUG -> buildDebug();
			case THEME -> buildTheme();
			case AXO -> buildAxo();
			case EXTRAS -> buildExtras();
		}
		if (nextColumn != 0) {
			nextRow++;
		}
		contentHeight = nextRow * ROW;
		applyScroll();

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
			.bounds(width / 2 - 100, height - 26, 200, 20).build());
	}

	private int columnX(int column) {
		return width / 2 - COLUMN_WIDTH - 4 + column * (COLUMN_WIDTH + 8);
	}

	/** Fügt ein Widget in das 2-spaltige Raster ein. */
	private <T extends AbstractWidget> T add(T widget) {
		widget.setX(columnX(nextColumn));
		widget.setWidth(COLUMN_WIDTH);
		place(widget);
		if (++nextColumn == 2) {
			nextColumn = 0;
			nextRow++;
		}
		return widget;
	}

	/** Fügt ein Widget über die volle Breite ein. */
	private <T extends AbstractWidget> T addWide(T widget) {
		if (nextColumn != 0) {
			nextColumn = 0;
			nextRow++;
		}
		widget.setX(columnX(0));
		widget.setWidth(COLUMN_WIDTH * 2 + 8);
		place(widget);
		nextRow++;
		return widget;
	}

	private void place(AbstractWidget widget) {
		int y = CONTENT_TOP + nextRow * ROW;
		widget.setY(y);
		content.add(widget);
		contentY.add(y);
		addRenderableWidget(widget);
	}

	private void skipToNewRow() {
		if (nextColumn != 0) {
			nextColumn = 0;
			nextRow++;
		}
	}

	private CycleButton<Boolean> toggle(String key, boolean value, Consumer<Boolean> setter) {
		return toggle(key, value, setter, false);
	}

	private CycleButton<Boolean> toggle(String key, boolean value, Consumer<Boolean> setter, boolean needsReload) {
		return add(CycleButton.onOffBuilder(value).create(0, 0, COLUMN_WIDTH, 20, Component.translatable(key), (b, v) -> {
			setter.accept(v);
			if (needsReload) {
				resourcesDirty = true;
			}
		}));
	}

	private ColorButton color(String key, IntSupplier getter, IntConsumer setter) {
		return color(key, getter, setter, false);
	}

	private ColorButton color(String key, IntSupplier getter, IntConsumer setter, boolean needsReload) {
		IntConsumer wrapped = needsReload ? v -> {
			if (v != getter.getAsInt()) {
				resourcesDirty = true;
			}
			setter.accept(v);
		} : setter;
		return add(new ColorButton(0, 0, COLUMN_WIDTH, Component.translatable(key), getter, wrapped, this));
	}

	private LuSlider slider(String key, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
		return add(new LuSlider(0, 0, COLUMN_WIDTH, Component.translatable(key), min, max, step, getter, setter));
	}

	private Button button(String key, Button.OnPress onPress) {
		return add(Button.builder(Component.translatable(key), onPress).bounds(0, 0, COLUMN_WIDTH, 20).build());
	}

	// ------------------------------------------------------------------ Tabs

	private void buildGeneral() {
		LuConfig c = LuConfig.get();
		addWide(Button.builder(Component.translatable("luhub.config.open_hud_editor"), b -> minecraft.gui.setScreen(new HudEditorScreen(this)))
			.bounds(0, 0, 100, 20).build());
		addWide(Button.builder(Component.translatable("luhub.config.open_debug_editor"), b -> minecraft.gui.setScreen(new DebugEditorScreen(this)))
			.bounds(0, 0, 100, 20).build());
		toggle("luhub.config.hud_enabled", c.hudEnabled, v -> c.hudEnabled = v);
		toggle("luhub.config.debug_enabled", c.debugEnabled, v -> c.debugEnabled = v);
		toggle("luhub.config.theme_enabled", c.themeEnabled, v -> c.themeEnabled = v, true);
		toggle("luhub.config.axo_enabled", c.axoEnabled, v -> c.axoEnabled = v);
		skipToNewRow();
		addWide(Button.builder(Component.translatable("luhub.config.controls"), b -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)))
			.bounds(0, 0, 100, 20).build());
	}

	private void buildHud() {
		LuConfig c = LuConfig.get();
		HudElementConfig ec = c.hud(selectedHud);
		add(CycleButton.<HudElementType>builder(HudElementType::label, selectedHud)
			.withValues(java.util.Arrays.stream(HudElementType.values()).filter(HudElementType::available).toList())
			.create(0, 0, COLUMN_WIDTH, 20, Component.translatable("luhub.config.element"), (b, v) -> {
				selectedHud = v;
				rebuildWidgets();
			}));
		toggle("luhub.config.visible", ec.visible, v -> ec.visible = v);
		if (selectedHud.styleable) {
			add(CycleButton.<BarStyle>builder(BarStyle::label, ec.style)
				.withValues(BarStyle.values())
				.create(0, 0, COLUMN_WIDTH, 20, Component.translatable("luhub.config.style"), (b, v) -> {
					ec.style = v;
					rebuildWidgets();
				}));
			toggle("luhub.config.sparkle", ec.sparkle, v -> ec.sparkle = v);
			if (ec.style == BarStyle.CUSTOM) {
				color("luhub.config.fill_color", () -> ec.fillColor, v -> ec.fillColor = v);
			}
			if (ec.style != BarStyle.VANILLA) {
				color("luhub.config.empty_color", () -> ec.emptyColor, v -> ec.emptyColor = v);
				color("luhub.config.outline_color", () -> ec.outlineColor, v -> ec.outlineColor = v);
			}
		}
		slider("luhub.config.scale", 0.25f, 4f, 0.05f, () -> ec.scale, v -> ec.scale = v);
		button("luhub.config.reset_position", b -> {
			ec.dx = 0;
			ec.dy = 0;
			ec.scale = 1f;
			rebuildWidgets();
		});
		skipToNewRow();
		button("luhub.config.apply_style_all", b -> {
			for (HudElementType t : HudElementType.values()) {
				if (t.styleable) {
					HudElementConfig other = c.hud(t);
					other.style = ec.style;
					other.sparkle = ec.sparkle;
				}
			}
		}).active = selectedHud.styleable;
		button("luhub.config.reset_hud_layout", b -> {
			LuConfig.resetHudLayout();
			rebuildWidgets();
		});
		addWide(Button.builder(Component.translatable("luhub.config.open_hud_editor"), b -> minecraft.gui.setScreen(new HudEditorScreen(this)))
			.bounds(0, 0, 100, 20).build());
	}

	private void buildDebug() {
		LuConfig c = LuConfig.get();
		toggle("luhub.config.debug_enabled", c.debugEnabled, v -> c.debugEnabled = v);
		f3KeyButton = add(Button.builder(Component.empty(), b -> {
			listeningForF3Key = true;
			updateF3KeyButton();
		}).bounds(0, 0, COLUMN_WIDTH, 20).build());
		updateF3KeyButton();
		toggle("luhub.config.debug_colorize", c.debugColorize, v -> c.debugColorize = v);
		toggle("luhub.config.debug_titles", c.debugShowTitles, v -> c.debugShowTitles = v);
		toggle("luhub.config.debug_shadow", c.debugShadow, v -> c.debugShadow = v);
		toggle("luhub.config.debug_fps_colors", c.debugFpsColors, v -> c.debugFpsColors = v);
		slider("luhub.config.scale", 0.25f, 3f, 0.05f, () -> c.debugScale, v -> c.debugScale = v);
		color("luhub.config.debug_background", () -> c.debugBackground, v -> c.debugBackground = v);
		color("luhub.config.debug_label", () -> c.debugLabelColor, v -> c.debugLabelColor = v);
		color("luhub.config.debug_value", () -> c.debugValueColor, v -> c.debugValueColor = v);
		color("luhub.config.debug_number", () -> c.debugNumberColor, v -> c.debugNumberColor = v);
		color("luhub.config.debug_header", () -> c.debugHeaderColor, v -> c.debugHeaderColor = v);
		button("luhub.config.reset_debug_layout", b -> LuConfig.resetDebugLayout());
		button("luhub.config.debug_options", b -> minecraft.gui.setScreen(new net.minecraft.client.gui.screens.debug.DebugOptionsScreen()));
		addWide(Button.builder(Component.translatable("luhub.config.open_debug_editor"), b -> minecraft.gui.setScreen(new DebugEditorScreen(this)))
			.bounds(0, 0, 100, 20).build());
	}

	private void updateF3KeyButton() {
		if (f3KeyButton == null) {
			return;
		}
		KeyMapping key = minecraft.options.keyDebugOverlay;
		Component keyName = listeningForF3Key
			? Component.literal("> ").append(key.getTranslatedKeyMessage()).append(" <")
			: key.getTranslatedKeyMessage();
		f3KeyButton.setMessage(Component.translatable("luhub.config.f3_key").append(": ").append(keyName));
	}

	private void buildTheme() {
		LuConfig c = LuConfig.get();
		toggle("luhub.config.theme_enabled", c.themeEnabled, v -> c.themeEnabled = v, true);
		toggle("luhub.config.theme_all_guis", c.themeAllGuis, v -> c.themeAllGuis = v, true);
		toggle("luhub.config.shulker_colors", c.shulkerColors, v -> c.shulkerColors = v);
		toggle("luhub.config.theme_hotbar", c.themeHotbar, v -> c.themeHotbar = v, true);
		toggle("luhub.config.theme_recipe_book", c.themeRecipeBook, v -> c.themeRecipeBook = v, true);
		toggle("luhub.config.theme_modded", c.themeModdedContainers, v -> c.themeModdedContainers = v, true);
		toggle("luhub.config.night_background", c.nightBackground, v -> c.nightBackground = v);
		toggle("luhub.config.moon", c.moonDecoration, v -> c.moonDecoration = v);
		toggle("luhub.config.stars", c.stars, v -> c.stars = v, true);
		slider("luhub.config.star_density", 0f, 4f, 0.1f, () -> c.starDensity, v -> {
			c.starDensity = v;
			resourcesDirty = true;
		});
		color("luhub.config.color_background", () -> c.colorBackground, v -> c.colorBackground = v, true);
		color("luhub.config.color_slot", () -> c.colorSlot, v -> c.colorSlot = v, true);
		color("luhub.config.color_highlight", () -> c.colorHighlight, v -> c.colorHighlight = v, true);
		color("luhub.config.color_shadow", () -> c.colorShadow, v -> c.colorShadow = v, true);
		color("luhub.config.color_border", () -> c.colorBorder, v -> c.colorBorder = v, true);
		color("luhub.config.color_star", () -> c.colorStar, v -> c.colorStar = v, true);
		color("luhub.config.color_label", () -> c.colorLabel, v -> c.colorLabel = v);
		color("luhub.config.color_moon", () -> c.colorMoon, v -> c.colorMoon = v);
		color("luhub.config.color_sky_top", () -> c.colorSkyTop, v -> c.colorSkyTop = v);
		color("luhub.config.color_sky_bottom", () -> c.colorSkyBottom, v -> c.colorSkyBottom = v);
		addWide(Button.builder(Component.translatable("luhub.config.reset_colors"), b -> {
			LuConfig.resetTheme();
			resourcesDirty = true;
			rebuildWidgets();
		}).bounds(0, 0, 100, 20).build());
	}

	private void buildExtras() {
		LuConfig c = LuConfig.get();
		// Hunger-Infos (wie AppleSkin)
		toggle("luhub.config.food_tooltip", c.foodTooltip, v -> c.foodTooltip = v).active = !ExternalMods.APPLESKIN;
		toggle("luhub.config.food_saturation", c.foodSaturationOverlay, v -> c.foodSaturationOverlay = v).active = !ExternalMods.APPLESKIN;
		toggle("luhub.config.food_exhaustion", c.foodExhaustionUnderlay, v -> c.foodExhaustionUnderlay = v).active = !ExternalMods.APPLESKIN;
		toggle("luhub.config.food_preview", c.foodHeldPreview, v -> c.foodHeldPreview = v).active = !ExternalMods.APPLESKIN;
		toggle("luhub.config.food_health_preview", c.foodHealthPreview, v -> c.foodHealthPreview = v).active = !ExternalMods.APPLESKIN;
		// Shulker-Vorschau (wie Shulker Box Tooltip)
		toggle("luhub.config.shulker_preview", c.shulkerPreview, v -> c.shulkerPreview = v).active = !ExternalMods.SHULKER_BOX_TOOLTIP;
		toggle("luhub.config.shulker_hide_lines", c.shulkerPreviewHideLines, v -> c.shulkerPreviewHideLines = v).active = !ExternalMods.SHULKER_BOX_TOOLTIP;
		// Sortieren (wie Sorted)
		toggle("luhub.config.sort_buttons", c.sortButtons, v -> c.sortButtons = v).active = !ExternalMods.SORTED;
		toggle("luhub.config.sort_hotbar", c.sortHotbar, v -> c.sortHotbar = v).active = !ExternalMods.SORTED;
		// Rüstungs-HUD (wie uku's Armor HUD)
		boolean armor = !ExternalMods.UKUS_ARMOR_HUD;
		HudElementConfig armorHud = c.hud(HudElementType.ARMOR_HUD);
		toggle("luhub.config.armor_hud", armorHud.visible, v -> armorHud.visible = v).active = armor;
		add(CycleButton.<dev.lunaos.luhub.config.ArmorHudOrientation>builder(dev.lunaos.luhub.config.ArmorHudOrientation::label, c.armorOrientation)
			.withValues(dev.lunaos.luhub.config.ArmorHudOrientation.values())
			.create(0, 0, COLUMN_WIDTH, 20, Component.translatable("luhub.config.armor_orientation"), (b, v) -> c.armorOrientation = v)).active = armor;
		add(CycleButton.<dev.lunaos.luhub.config.ArmorDurability>builder(dev.lunaos.luhub.config.ArmorDurability::label, c.armorDurability)
			.withValues(dev.lunaos.luhub.config.ArmorDurability.values())
			.create(0, 0, COLUMN_WIDTH, 20, Component.translatable("luhub.config.armor_durability"), (b, v) -> c.armorDurability = v)).active = armor;
		toggle("luhub.config.armor_hands", c.armorShowHands, v -> c.armorShowHands = v).active = armor;
		toggle("luhub.config.armor_empty", c.armorShowEmpty, v -> c.armorShowEmpty = v).active = armor;
		toggle("luhub.config.armor_sound", c.armorWarnSound, v -> c.armorWarnSound = v).active = armor;
		slider("luhub.config.armor_warn", 0f, 50f, 1f, () -> (float) c.armorWarnPercent, v -> c.armorWarnPercent = Math.round(v)).active = armor;
		skipToNewRow();
		if (ExternalMods.APPLESKIN || ExternalMods.SHULKER_BOX_TOOLTIP || ExternalMods.SORTED || ExternalMods.UKUS_ARMOR_HUD) {
			addWide(Button.builder(Component.translatable("luhub.config.external_hint"), b -> { }).bounds(0, 0, 100, 20).build()).active = false;
		}
	}

	private void buildAxo() {
		LuConfig c = LuConfig.get();
		toggle("luhub.config.axo_enabled", c.axoEnabled, v -> c.axoEnabled = v);
		toggle("luhub.config.axo_only_inventory", c.axoOnlyInventory, v -> c.axoOnlyInventory = v);
		toggle("luhub.config.axo_animated", c.axoAnimated, v -> c.axoAnimated = v);
	}

	// ------------------------------------------------------------------ Scrollen

	private int visibleBottom() {
		return height - 32;
	}

	private void applyScroll() {
		int maxScroll = Math.max(0, CONTENT_TOP + contentHeight - visibleBottom());
		scroll = Mth.clamp(scroll, 0, maxScroll);
		for (int i = 0; i < content.size(); i++) {
			AbstractWidget w = content.get(i);
			int y = contentY.get(i) - scroll;
			w.setY(y);
			w.visible = y >= CONTENT_TOP - 4 && y + w.getHeight() <= visibleBottom() + 4;
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (super.mouseScrolled(x, y, scrollX, scrollY)) {
			return true;
		}
		scroll -= (int) (scrollY * ROW);
		applyScroll();
		return true;
	}

	// ------------------------------------------------------------------ Eingabe

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (listeningForF3Key) {
			KeyMapping key = minecraft.options.keyDebugOverlay;
			key.setKey(event.isEscape() ? key.getDefaultKey() : InputConstants.getKey(event));
			KeyMapping.resetMapping();
			minecraft.options.save();
			listeningForF3Key = false;
			updateF3KeyButton();
			return true;
		}
		return super.keyPressed(event);
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		LuConfig c = LuConfig.get();
		ThemeDecor.extractMoon(graphics, width / 2 - font.width(title) / 2 - 16, 5, ThemeDecor.MOON_SIZE, c.colorMoon, false);
		graphics.centeredText(font, title, width / 2, 7, 0xFFFFFFFF);

		int maxScroll = Math.max(0, CONTENT_TOP + contentHeight - visibleBottom());
		if (maxScroll > 0) {
			int trackX = columnX(1) + COLUMN_WIDTH + 6;
			int trackH = visibleBottom() - CONTENT_TOP;
			int thumbH = Math.max(16, trackH * trackH / (trackH + maxScroll));
			int thumbY = CONTENT_TOP + (trackH - thumbH) * scroll / maxScroll;
			graphics.fill(trackX, CONTENT_TOP, trackX + 3, visibleBottom(), 0x40FFFFFF);
			graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFB9C0FF);
		}

		if (tab == Tab.AXO) {
			int previewY = CONTENT_TOP + 2 * ROW + 10;
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			int scale = 4;
			pose.translate(width / 2f - AxoRenderer.FRAME_WIDTH * scale / 2f, previewY);
			pose.scale(scale, scale);
			boolean hovered = mouseX >= width / 2 - 56 && mouseX < width / 2 + 56 && mouseY >= previewY && mouseY < previewY + 76;
			AxoRenderer.extract(graphics, 0, 6, hovered, true);
			pose.popMatrix();
			graphics.centeredText(font, Component.translatable("luhub.config.axo_hint"), width / 2, previewY + 92, 0xFFB9C0FF);
		} else if (tab == Tab.THEME && resourcesDirty) {
			graphics.centeredText(font, Component.translatable("luhub.config.reload_hint"), width / 2, height - 40, 0xFFFFE08A);
		}
	}

	@Override
	public void onClose() {
		LuConfig.save();
		if (resourcesDirty) {
			resourcesDirty = false;
			minecraft.reloadResourcePacks();
		}
		minecraft.gui.setScreen(parent);
	}

	@Override
	public void removed() {
		LuConfig.save();
	}
}
