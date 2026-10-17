package dev.lunaos.luhub.theme;

import com.mojang.blaze3d.platform.NativeImage;
import dev.lunaos.luhub.LuHub;
import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shulkerkisten-GUI in der Farbe der geöffneten Shulkerkiste – im Nachthimmel-Stil mit Sternen und Lichtkanten.
 * Für jede Farbe wird einmal eine eigene Textur aus der Vanilla-Textur erzeugt.
 */
public final class ShulkerTheme {
	private static final Map<Integer, Identifier> TEXTURES = new HashMap<>();
	private static int cacheKey;

	private ShulkerTheme() {
	}

	/**
	 * Farbe (RGB) der geöffneten Shulkerkiste – oder null bei ungefärbten Kisten.
	 * Reihenfolge: Fenstertitel ("Rote Shulkerkiste"), angeschauter Block, Shulkerkiste in der Hand
	 * (für Mods/Server, die Shulker direkt aus dem Inventar öffnen).
	 */
	public static Integer colorFor(Component title) {
		DyeColor color = fromTitle(title);
		if (color == null) {
			color = fromTargetedBlock();
		}
		if (color == null) {
			color = fromHeldItem();
		}
		return color == null ? null : color.getTextureDiffuseColor() & 0xFFFFFF;
	}

	private static DyeColor fromTitle(Component title) {
		if (title != null && title.getContents() instanceof TranslatableContents t) {
			String key = t.getKey();
			if (key.startsWith("block.minecraft.") && key.endsWith("_shulker_box")) {
				String name = key.substring("block.minecraft.".length(), key.length() - "_shulker_box".length());
				return DyeColor.byName(name, null);
			}
		}
		return null;
	}

	private static DyeColor fromTargetedBlock() {
		Minecraft mc = Minecraft.getInstance();
		HitResult hit = mc.hitResult;
		if (mc.level == null || !(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		return mc.level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof ShulkerBoxBlock shulker ? shulker.getColor() : null;
	}

	private static DyeColor fromHeldItem() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return null;
		}
		for (ItemStack stack : new ItemStack[]{mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
			if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock shulker) {
				return shulker.getColor();
			}
		}
		return null;
	}

	public static boolean enabled(Integer rgb) {
		LuConfig c = LuConfig.get();
		return rgb != null && c.themeEnabled && c.shulkerColors;
	}

	/** Gut lesbare Überschriften-Farbe für den eingefärbten Hintergrund. */
	public static int labelColor(int rgb) {
		NightTheme.Palette palette = NightTheme.Palette.tinted(rgb);
		int bg = palette.background();
		int lum = (((bg >> 16) & 0xFF) * 30 + ((bg >> 8) & 0xFF) * 59 + (bg & 0xFF) * 11) / 100;
		return lum > 120 ? NightTheme.darken(0xFF000000 | rgb, 0.8f) : NightTheme.lerp(0xFF000000 | rgb, 0xFFFFFFFF, 0.75f);
	}

	/** Liefert die eingefärbte Variante der Shulker-Textur (oder null, falls nicht möglich). */
	public static Identifier texture(Identifier vanilla, int rgb) {
		LuConfig c = LuConfig.get();
		int key = Objects.hash(NightTheme.generation, c.colorBackground, c.colorStar, c.stars, c.starDensity);
		Minecraft mc = Minecraft.getInstance();
		if (key != cacheKey) {
			TEXTURES.values().forEach(mc.getTextureManager()::release);
			TEXTURES.clear();
			cacheKey = key;
		}
		Identifier cached = TEXTURES.get(rgb);
		if (cached != null) {
			return cached;
		}
		Optional<Resource> resource = mc.getResourceManager().getResource(vanilla);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream in = resource.get().open()) {
			NativeImage image = NativeImage.read(in);
			NightTheme.apply(image, vanilla.hashCode(), NightTheme.Kind.PANEL, NightTheme.Palette.tinted(rgb));
			Identifier id = Identifier.fromNamespaceAndPath(LuHub.MOD_ID, String.format(Locale.ROOT, "themed/shulker_%06x", rgb));
			mc.getTextureManager().register(id, new DynamicTexture(id::toString, image));
			TEXTURES.put(rgb, id);
			return id;
		} catch (IOException e) {
			LuHub.LOGGER.warn("Konnte Shulker-Textur nicht einfärben", e);
			return null;
		}
	}
}
