package dev.lunaos.luhub.debug;

import dev.lunaos.luhub.config.Anchor;
import dev.lunaos.luhub.config.DebugPanelConfig;
import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eigene Darstellung der F3-Ansicht: jeder Vanilla-Debug-Eintrag (FPS, Position, Biom, ...) wird zu einem
 * eigenen, farbig markierten Panel, das frei verschoben und skaliert werden kann.
 * Die Inhalte kommen direkt von den Vanilla-Einträgen, es fehlt also nichts und Mod-Einträge funktionieren auch.
 */
public final class LuDebugOverlay {
	private static final int LINE_HEIGHT = 9;
	private static final int PADDING = 3;
	private static final int ACCENT = 2;
	private static final int MARGIN = 2;
	private static final int[] AUTO_COLORS = {
		0xFF8FB8FF, 0xFFFF9BD2, 0xFF9DF5C4, 0xFFFFD27F, 0xFFC7A6FF, 0xFF7FE7F0, 0xFFFFA98B, 0xFFE8F08F
	};
	/** Einträge, die standardmäßig rechts stehen (wie in Vanilla). */
	private static final Set<String> RIGHT_SIDE = Set.of(
		"memory", "detailed_memory", "system_specs", "gpu_utilization",
		"looking_at_block_state", "looking_at_block_tags", "looking_at_fluid_state", "looking_at_fluid_tags",
		"looking_at_entity", "looking_at_entity_tags"
	);

	private static final Map<Identifier, List<String>> frameLines = new LinkedHashMap<>();
	private static final Map<Identifier, List<String>> lastLines = new LinkedHashMap<>();
	private static final Map<DebugScreenEntry, Identifier> idsByEntry = new IdentityHashMap<>();
	private static boolean capturing;
	/** Solange der F3-Editor offen ist, zeichnet nur der Editor die Panels. */
	public static boolean editorOpen;

	private LuDebugOverlay() {
	}

	public record PanelRect(Identifier id, int x, int y, int width, int height, float scale) {
		public boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx < x + width && my < y + height;
		}
	}

	public static boolean isActive() {
		return LuConfig.get().debugEnabled;
	}

	public static void beginFrame() {
		frameLines.clear();
		capturing = true;
	}

	/** Umhüllt den Vanilla-Displayer, damit wir mitbekommen, welche Zeilen zu welchem Eintrag gehören. */
	public static DebugScreenDisplayer capture(DebugScreenEntry entry, DebugScreenDisplayer delegate) {
		Identifier id = idOf(entry);
		List<String> lines = frameLines.computeIfAbsent(id, k -> new ArrayList<>());
		return new DebugScreenDisplayer() {
			@Override
			public void addPriorityLine(String line) {
				lines.add(line);
				delegate.addPriorityLine(line);
			}

			@Override
			public void addLine(String line) {
				lines.add(line);
				delegate.addLine(line);
			}

			@Override
			public void addToGroup(Identifier group, Collection<String> groupLines) {
				lines.addAll(groupLines);
				delegate.addToGroup(group, groupLines);
			}

			@Override
			public void addToGroup(Identifier group, String line) {
				lines.add(line);
				delegate.addToGroup(group, line);
			}
		};
	}

	/** Zeichnet alle Panels der aktuellen Frame (wird am Ende von DebugScreenOverlay aufgerufen). */
	public static void endFrame(GuiGraphicsExtractor graphics) {
		if (!capturing) {
			return;
		}
		capturing = false;
		if (!frameLines.isEmpty()) {
			// letzte echte F3-Inhalte merken, damit der Editor sie als Vorschau zeigen kann
			lastLines.clear();
			frameLines.forEach((id, lines) -> lastLines.put(id, List.copyOf(lines)));
		}
		extract(graphics, frameLines, null, null);
	}

	/** Die zuletzt angezeigten Zeilen, für die Vorschau im Editor. */
	public static Map<Identifier, List<String>> previewLines() {
		Map<Identifier, List<String>> result = new LinkedHashMap<>(lastLines);
		result.values().removeIf(List::isEmpty);
		if (result.isEmpty()) {
			result.put(DebugScreenEntries.FPS, List.of("120 fps T: 120 vsync"));
			result.put(DebugScreenEntries.PLAYER_POSITION, List.of("XYZ: 12.500 / 64.00000 / -8.300", "Block: 12 64 -9 [12 0 7]", "Facing: north (Towards negative Z) (0.0 / 0.0)"));
			result.put(DebugScreenEntries.BIOME, List.of("Biome: minecraft:plains"));
			result.put(DebugScreenEntries.MEMORY, List.of("Mem: 42% 1700/4096MB", "Allocation rate: 120MB /s"));
		}
		return result;
	}

	private static Identifier idOf(DebugScreenEntry entry) {
		Identifier id = idsByEntry.get(entry);
		if (id == null) {
			idsByEntry.clear();
			DebugScreenEntries.allEntries().forEach((key, value) -> idsByEntry.put(value, key));
			id = idsByEntry.getOrDefault(entry, Identifier.fromNamespaceAndPath("luhub", "unknown"));
		}
		return id;
	}

	/**
	 * Legt die Panels aus und zeichnet sie.
	 *
	 * @param outRects falls nicht null, werden hier die Rechtecke aller Panels gesammelt (für den Editor)
	 * @param selected Panel, das im Editor hervorgehoben werden soll
	 */
	public static void extract(GuiGraphicsExtractor graphics, Map<Identifier, List<String>> lines,
							   List<PanelRect> outRects, Identifier selected) {
		LuConfig config = LuConfig.get();
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int screenW = graphics.guiWidth();
		int screenH = graphics.guiHeight();
		boolean title = config.debugShowTitles;

		// 1. Größe jedes Panels bestimmen
		List<Layout> layouts = new ArrayList<>();
		for (Map.Entry<Identifier, List<String>> e : lines.entrySet()) {
			List<String> panelLines = e.getValue();
			if (panelLines.isEmpty()) {
				continue;
			}
			Identifier id = e.getKey();
			DebugPanelConfig pc = config.panel(id.toString());
			if (pc.hidden && outRects == null) {
				continue;
			}
			float scale = config.debugScale * pc.scale;
			int innerW = 0;
			for (String line : panelLines) {
				innerW = Math.max(innerW, font.width(stripFormatting(line)));
			}
			if (title) {
				innerW = Math.max(innerW, font.width(title(id)));
			}
			int w = innerW + ACCENT + PADDING * 2;
			int h = (panelLines.size() + (title ? 1 : 0)) * LINE_HEIGHT + PADDING;
			layouts.add(new Layout(id, pc, panelLines, w, h, scale, Math.round(w * scale), Math.round(h * scale)));
		}

		// 2. Positionen: frei platzierte zuerst, dann linke Spalte, dann rechte Spalte (weicht Überlappungen aus)
		List<int[]> taken = new ArrayList<>();
		for (Layout l : layouts) {
			if (l.pc.placed) {
				Anchor a = l.pc.anchor;
				l.x = a.baseX(screenW) + l.pc.x - Math.round(a.pivotX() * l.sw);
				l.y = a.baseY(screenH) + l.pc.y - Math.round(a.pivotY() * l.sh);
				taken.add(new int[]{l.x, l.y, l.sw, l.sh});
			}
		}
		int leftY = MARGIN;
		for (Layout l : layouts) {
			if (!l.pc.placed && !RIGHT_SIDE.contains(l.id.getPath())) {
				l.x = MARGIN;
				l.y = freeY(MARGIN, leftY, l.sw, l.sh, taken);
				leftY = l.y + l.sh + 2;
				taken.add(new int[]{l.x, l.y, l.sw, l.sh});
			}
		}
		int rightY = MARGIN;
		for (Layout l : layouts) {
			if (!l.pc.placed && RIGHT_SIDE.contains(l.id.getPath())) {
				l.x = Math.max(MARGIN, screenW - MARGIN - l.sw);
				l.y = freeY(l.x, rightY, l.sw, l.sh, taken);
				rightY = l.y + l.sh + 2;
				taken.add(new int[]{l.x, l.y, l.sw, l.sh});
			}
		}

		// 3. Zeichnen
		for (Layout l : layouts) {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(l.x, l.y);
			pose.scale(l.scale, l.scale);
			extractPanel(graphics, font, l.id, l.pc, l.lines, l.w, l.h, title, l.pc.hidden);
			pose.popMatrix();

			if (outRects != null) {
				outRects.add(new PanelRect(l.id, l.x, l.y, l.sw, l.sh, l.scale));
				if (l.id.equals(selected)) {
					graphics.outline(l.x - 1, l.y - 1, l.sw + 2, l.sh + 2, 0xFFFFFFFF);
				}
			}
		}
	}

	private static final class Layout {
		final Identifier id;
		final DebugPanelConfig pc;
		final List<String> lines;
		final int w;
		final int h;
		final float scale;
		final int sw;
		final int sh;
		int x;
		int y;

		Layout(Identifier id, DebugPanelConfig pc, List<String> lines, int w, int h, float scale, int sw, int sh) {
			this.id = id;
			this.pc = pc;
			this.lines = lines;
			this.w = w;
			this.h = h;
			this.scale = scale;
			this.sw = sw;
			this.sh = sh;
		}
	}

	/** Erste Y-Position ab startY, an der ein Panel keine bereits belegte Fläche überdeckt. */
	private static int freeY(int x, int startY, int w, int h, List<int[]> taken) {
		int y = startY;
		boolean moved = true;
		while (moved) {
			moved = false;
			for (int[] r : taken) {
				if (x < r[0] + r[2] && x + w > r[0] && y < r[1] + r[3] && y + h > r[1]) {
					y = r[1] + r[3] + 2;
					moved = true;
				}
			}
		}
		return y;
	}

	private static void extractPanel(GuiGraphicsExtractor graphics, Font font, Identifier id, DebugPanelConfig pc,
									 List<String> lines, int w, int h, boolean title, boolean hidden) {
		LuConfig config = LuConfig.get();
		int accent = accentColor(id, pc);
		int bg = config.debugBackground;
		if (hidden) {
			bg = ARGB.color(60, bg & 0xFFFFFF);
			accent = ARGB.color(90, accent & 0xFFFFFF);
		}
		graphics.fill(0, 0, w, h, bg);
		graphics.fill(0, 0, ACCENT, h, accent);
		int textX = ACCENT + PADDING;
		int y = 2;
		if (title) {
			graphics.text(font, Component.literal(title(id)).withStyle(s -> s.withBold(true)), textX, y, accent | 0xFF000000, config.debugShadow);
			y += LINE_HEIGHT;
		}
		for (String line : lines) {
			Component text = config.debugColorize ? DebugColorizer.colorize(line, accent) : Component.literal(stripFormatting(line));
			int base = config.debugColorize ? config.debugValueColor : 0xFFE0E0E0;
			if (hidden) {
				base = ARGB.color(110, base & 0xFFFFFF);
			}
			graphics.text(font, text, textX, y, base, config.debugShadow);
			y += LINE_HEIGHT;
		}
	}

	public static int accentColor(Identifier id, DebugPanelConfig pc) {
		if (pc.color != 0) {
			return pc.color;
		}
		return AUTO_COLORS[Math.floorMod(id.getPath().hashCode(), AUTO_COLORS.length)];
	}

	public static String title(Identifier id) {
		String key = "luhub.debug." + id.getPath();
		if (Language.getInstance().has(key)) {
			return I18n.get(key);
		}
		String[] parts = id.getPath().split("_");
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (!p.isEmpty()) {
				sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
			}
		}
		return sb.toString().trim();
	}

	static String stripFormatting(String line) {
		return line.replaceAll("§.", "");
	}
}
