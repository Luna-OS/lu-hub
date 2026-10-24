package dev.lunaos.luhub.compat.rei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;

/**
 * Erz-Verteilung der Oberwelt und des Nethers – Werte aus den Vanilla-Platzierungen (OrePlacements) von 26.3.
 * Jedes Erz besteht aus mehreren "Bändern": gleichmäßig verteilt oder dreieckig mit Spitze in der Mitte.
 */
public enum OreData {
	DIAMOND(0xFF5DECF5, Dim.OVERWORLD, -59, -64, -50, Items.DIAMOND, List.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE),
		tri(-144, 16, 7), uni(-64, -4, 2), tri(-144, 16, 1 / 9f), tri(-144, 16, 4)),
	REDSTONE(0xFFFF4A4A, Dim.OVERWORLD, -59, -64, -50, Items.REDSTONE, List.of(Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE),
		uni(-64, 15, 4), tri(-96, -32, 8)),
	LAPIS(0xFF4A72F0, Dim.OVERWORLD, 0, -16, 16, Items.LAPIS_LAZULI, List.of(Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE),
		tri(-32, 32, 2), uni(-64, 64, 4)),
	GOLD(0xFFFFD84A, Dim.OVERWORLD, -16, -32, 0, Items.RAW_GOLD, List.of(Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE),
		tri(-64, 32, 4), uni(-64, -48, 0.5f), special(32, 256, 50, false)),
	IRON(0xFFE8CDB8, Dim.OVERWORLD, 16, 0, 32, Items.RAW_IRON, List.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE),
		tri(80, 384, 90), tri(-24, 56, 10), uni(-64, 72, 10)),
	COPPER(0xFFE8814A, Dim.OVERWORLD, 48, 32, 64, Items.RAW_COPPER, List.of(Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE),
		tri(-16, 112, 16), special(-16, 112, 16, true)),
	COAL(0xFF9A9AB0, Dim.OVERWORLD, 95, 80, 136, Items.COAL, List.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE),
		uni(136, 320, 30), tri(0, 192, 20)),
	EMERALD(0xFF4AE07A, Dim.OVERWORLD, 232, 180, 256, Items.EMERALD, List.of(Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE),
		special(-16, 480, 100, true)),
	ANCIENT_DEBRIS(0xFFB07A64, Dim.NETHER, 15, 8, 22, Items.NETHERITE_SCRAP, List.of(Items.ANCIENT_DEBRIS),
		tri(8, 24, 1), uni(8, 119, 1)),
	NETHER_QUARTZ(0xFFF2ECE4, Dim.NETHER, 60, 10, 117, Items.QUARTZ, List.of(Items.NETHER_QUARTZ_ORE),
		uni(10, 117, 16)),
	NETHER_GOLD(0xFFFFC93A, Dim.NETHER, 60, 10, 117, Items.GOLD_NUGGET, List.of(Items.NETHER_GOLD_ORE),
		uni(10, 117, 10));

	public enum Dim {
		OVERWORLD(-64, 320), NETHER(0, 128);

		public final int minY;
		public final int maxY;

		Dim(int minY, int maxY) {
			this.minY = minY;
			this.maxY = maxY;
		}

		public Component label() {
			return Component.translatable("luhub.ores.dim." + name().toLowerCase(Locale.ROOT));
		}
	}

	/**
	 * Ein Band der Verteilung. {@code special}: nur in bestimmten Biomen/Höhlen (Gold in Tafelbergen,
	 * große Kupferadern, Smaragd nur in Bergen) – wird schraffiert gezeichnet.
	 */
	public record Band(int min, int max, float count, boolean triangle, boolean special) {
		float density(int y) {
			if (y < min || y > max) {
				return 0f;
			}
			if (!triangle) {
				return count / (max - min + 1);
			}
			float half = (max - min) / 2f;
			float mid = (min + max) / 2f;
			return Math.max(0f, 1f - Math.abs(y - mid) / half) * count / half;
		}
	}

	public final int color;
	public final Dim dim;
	public final int bestY;
	public final int rangeMin;
	public final int rangeMax;
	public final Item drop;
	public final List<Item> blocks;
	public final List<Band> bands;

	OreData(int color, Dim dim, int bestY, int rangeMin, int rangeMax, Item drop, List<Item> blocks, Band... bands) {
		this.color = color;
		this.dim = dim;
		this.bestY = bestY;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.drop = drop;
		this.blocks = blocks;
		this.bands = List.of(bands);
	}

	private static Band uni(int min, int max, float count) {
		return new Band(min, max, count, false, false);
	}

	private static Band tri(int min, int max, float count) {
		return new Band(min, max, count, true, false);
	}

	private static Band special(int min, int max, float count, boolean triangle) {
		return new Band(min, max, count, triangle, true);
	}

	public float density(int y, boolean special) {
		float sum = 0f;
		for (Band band : bands) {
			if (band.special() == special) {
				sum += band.density(y);
			}
		}
		return sum;
	}

	/** Höchste Dichte im Bereich der Dimension (normal + besondere Bänder) – zum Skalieren der Kurve. */
	public float maxDensity() {
		float max = 0f;
		for (int y = dim.minY; y <= dim.maxY; y++) {
			max = Math.max(max, density(y, false) + density(y, true));
		}
		return max;
	}

	public Component label() {
		return Component.translatable("luhub.ores." + name().toLowerCase(Locale.ROOT));
	}

	public Component tip() {
		return Component.translatable("luhub.ores." + name().toLowerCase(Locale.ROOT) + ".tip");
	}

	public static List<OreData> of(Dim dim) {
		return java.util.Arrays.stream(values()).filter(o -> o.dim == dim).toList();
	}
}
