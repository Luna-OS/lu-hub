package dev.lunaos.luhub.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.lunaos.luhub.LuHub;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alle Einstellungen von Lu Hub. Wird als config/luhub.json gespeichert.
 * Farben sind ARGB-Ints (0xAARRGGBB).
 */
public final class LuConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("luhub.json");
	private static LuConfig instance = new LuConfig();

	// ---------- Nachthimmel-Theme (Inventar, Kisten, Öfen, Hotbar ...) ----------
	public boolean themeEnabled = true;
	/** Theme auch für Menüs, Knöpfe, Tooltips, Toasts, Fortschritte – nicht nur für Container. */
	public boolean themeAllGuis = true;
	/** Sternenhimmel und Mond über dem Panorama im Titelbildschirm. */
	public boolean titleScreenTheme = true;
	/** Inventar, Kisten, Öfen ... per Drag &amp; Drop an der Titelleiste (oder mit Alt) verschieben. */
	public boolean windowDrag = true;
	/** Verschiebung je Fenstertyp (Klassenname des Screens) in GUI-Pixeln: [x, y]. */
	public Map<String, int[]> windowOffsets = new java.util.HashMap<>();
	/** Shulkerkisten-GUI in der Farbe der Shulkerkiste (im Theme-Stil). */
	public boolean shulkerColors = true;
	/** Auch Container-Texturen anderer Mods einfärben (textures/gui/container/ in fremden Namespaces). */
	public boolean themeModdedContainers = false;
	public boolean themeHotbar = true;
	/** Ausgewählten Hotbar-Slot hell hinterlegen. */
	public boolean hotbarHighlight = true;
	public int colorSelectedSlot = 0x70B9C0FF;
	public boolean themeRecipeBook = true;
	public boolean nightBackground = true;
	public boolean moonDecoration = true;
	public boolean stars = true;
	public float starDensity = 1.0f;
	public int colorBackground = 0xFF262E5C;
	public int colorSlot = 0xFF181D42;
	public int colorHighlight = 0xFF969FEB;
	public int colorShadow = 0xFF161A3C;
	public int colorBorder = 0xFF07081A;
	public int colorStar = 0xFFFFF4C2;
	public int colorLabel = 0xFFD9DDFF;
	public int colorMoon = 0xFFFFF1B8;
	public int colorSkyTop = 0xD0050716;
	public int colorSkyBottom = 0xD01C1446;

	// ---------- Axo ----------
	public boolean axoEnabled = true;
	/** Axo nur im Spieler-Inventar statt überall, wo das grüne Rezeptbuch ist. */
	public boolean axoOnlyInventory = false;
	public boolean axoAnimated = true;

	// ---------- Extras (eingebaute Funktionen von AppleSkin, Shulker Box Tooltip, Sorted, Armor HUD) ----------
	public boolean foodTooltip = true;
	public boolean foodSaturationOverlay = true;
	public boolean foodExhaustionUnderlay = true;
	public boolean foodHeldPreview = true;
	public boolean foodHealthPreview = true;
	public boolean shulkerPreview = true;
	public boolean shulkerPreviewHideLines = true;
	public boolean sortButtons = true;
	public boolean sortHotbar = false;
	public ArmorHudOrientation armorOrientation = ArmorHudOrientation.HORIZONTAL;
	public ArmorDurability armorDurability = ArmorDurability.BAR;
	public boolean armorShowEmpty = false;
	public boolean armorShowHands = true;
	public int armorWarnPercent = 10;
	public boolean armorWarnSound = true;

	// ---------- HUD ----------
	public boolean hudEnabled = true;
	public Map<HudElementType, HudElementConfig> hud = new EnumMap<>(HudElementType.class);

	// ---------- F3 ----------
	public boolean debugEnabled = true;
	public boolean debugColorize = true;
	public boolean debugShowTitles = false;
	public boolean debugShadow = true;
	public boolean debugFpsColors = true;
	public float debugScale = 1.0f;
	public int debugBackground = 0xA00B0E24;
	public int debugLabelColor = 0xFFB9C0FF;
	public int debugValueColor = 0xFFF2F2FF;
	public int debugNumberColor = 0xFFFFE08A;
	public int debugHeaderColor = 0xFFFFB3E6;
	public Map<String, DebugPanelConfig> debugPanels = new LinkedHashMap<>();

	/** Für Einstellungs-Umstellungen zwischen Versionen. */
	public int configVersion = 0;
	private static final int CURRENT_VERSION = 2;

	public static LuConfig get() {
		return instance;
	}

	public HudElementConfig hud(HudElementType type) {
		return hud.computeIfAbsent(type, HudElementType::createDefault);
	}

	public DebugPanelConfig panel(String id) {
		return debugPanels.computeIfAbsent(id, k -> new DebugPanelConfig());
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				LuConfig loaded = GSON.fromJson(reader, LuConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				LuHub.LOGGER.error("Konnte {} nicht lesen, benutze Standardwerte", FILE, e);
				instance = new LuConfig();
			}
		}
		instance.fillDefaults();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LuHub.LOGGER.error("Konnte {} nicht speichern", FILE, e);
		}
	}

	/** Setzt einen Bereich auf Standardwerte zurück. */
	public static void resetTheme() {
		LuConfig d = new LuConfig();
		LuConfig c = instance;
		c.colorBackground = d.colorBackground;
		c.colorSlot = d.colorSlot;
		c.colorHighlight = d.colorHighlight;
		c.colorShadow = d.colorShadow;
		c.colorBorder = d.colorBorder;
		c.colorStar = d.colorStar;
		c.colorLabel = d.colorLabel;
		c.colorMoon = d.colorMoon;
		c.colorSkyTop = d.colorSkyTop;
		c.colorSkyBottom = d.colorSkyBottom;
		c.starDensity = d.starDensity;
	}

	public static void resetHudLayout() {
		for (HudElementType type : HudElementType.values()) {
			HudElementConfig e = instance.hud(type);
			e.dx = 0;
			e.dy = 0;
			e.scale = 1f;
		}
	}

	public static void resetDebugLayout() {
		for (DebugPanelConfig p : instance.debugPanels.values()) {
			p.placed = false;
			p.scale = 1f;
		}
	}

	private void fillDefaults() {
		Map<HudElementType, HudElementConfig> loadedHud = hud;
		hud = new EnumMap<>(HudElementType.class);
		if (loadedHud != null) {
			loadedHud.forEach((type, value) -> {
				if (type != null && value != null) {
					hud.put(type, value);
				}
			});
		}
		for (HudElementType type : HudElementType.values()) {
			HudElementConfig e = hud(type);
			if (e.style == null) {
				e.style = type.defaultStyle();
			}
			e.scale = Math.clamp(e.scale, 0.25f, 4f);
		}
		if (debugPanels == null) {
			debugPanels = new LinkedHashMap<>();
		}
		debugPanels.values().removeIf(java.util.Objects::isNull);
		if (windowOffsets == null) {
			windowOffsets = new java.util.HashMap<>();
		}
		windowOffsets.values().removeIf(v -> v == null || v.length != 2);
		for (DebugPanelConfig p : debugPanels.values()) {
			if (p.anchor == null) {
				p.anchor = Anchor.TOP_LEFT;
			}
			p.scale = Math.clamp(p.scale, 0.25f, 4f);
		}
		if (configVersion < 2) {
			// Rüstung hat jetzt Themes: alte Einstellungen von "Vanilla" auf den Nachthimmel-Stil umstellen
			HudElementConfig armor = hud(HudElementType.ARMOR);
			if (armor.style == BarStyle.VANILLA) {
				armor.style = BarStyle.NIGHT;
				armor.fillColor = HudElementType.ARMOR.defaultFill;
			}
		}
		configVersion = CURRENT_VERSION;
		debugScale = Math.clamp(debugScale, 0.25f, 4f);
		if (armorOrientation == null) {
			armorOrientation = ArmorHudOrientation.HORIZONTAL;
		}
		if (armorDurability == null) {
			armorDurability = ArmorDurability.BAR;
		}
		armorWarnPercent = Math.clamp(armorWarnPercent, 0, 100);
		starDensity = Math.clamp(starDensity, 0f, 4f);
	}
}
