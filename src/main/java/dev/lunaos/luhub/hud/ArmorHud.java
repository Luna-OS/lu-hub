package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.config.ArmorHudLayout;
import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rüstungs-HUD: zeigt Helm, Brustpanzer, Hose, Schuhe (und optional beide Hände) mit ihrer Haltbarkeit.
 * Wird ein Teil knapp, blinkt ein Warnzeichen und es ertönt einmal ein Hinweiston.
 */
public final class ArmorHud {
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final ItemStack[] PREVIEW = {
		new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE), new ItemStack(Items.IRON_LEGGINGS),
		new ItemStack(Items.GOLDEN_BOOTS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.SHIELD)
	};
	private static final Map<EquipmentSlot, Boolean> WARNED = new EnumMap<>(EquipmentSlot.class);

	static {
		PREVIEW[1].setDamageValue(PREVIEW[1].getMaxDamage() / 3);
		PREVIEW[2].setDamageValue(PREVIEW[2].getMaxDamage() * 19 / 20);
		PREVIEW[4].setDamageValue(PREVIEW[4].getMaxDamage() / 2);
	}

	private ArmorHud() {
	}

	private record Entry(EquipmentSlot slot, ItemStack stack) {
	}

	public static boolean extract(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Minecraft mc = Minecraft.getInstance();
		if (!(mc.getCameraEntity() instanceof Player player)) {
			return true;
		}
		LuConfig c = LuConfig.get();
		List<Entry> entries = collect(player, c);
		if (entries.isEmpty()) {
			return true;
		}
		int x0 = HudElementType.ARMOR_HUD.defaultX(graphics.guiWidth());
		int y0 = HudElementType.ARMOR_HUD.defaultY(graphics.guiHeight());
		boolean horizontal = ArmorHudLayout.horizontal();
		int cellH = ArmorHudLayout.cellHeight();
		for (int i = 0; i < entries.size(); i++) {
			int x = horizontal ? x0 + i * (ArmorHudLayout.SLOT + ArmorHudLayout.GAP) : x0;
			int y = horizontal ? y0 : y0 + i * (cellH + ArmorHudLayout.GAP);
			extractCell(graphics, mc.font, x, y, entries.get(i), ec, c);
		}
		return true;
	}

	private static List<Entry> collect(Player player, LuConfig c) {
		List<Entry> entries = new ArrayList<>();
		boolean preview = LuHud.previewMode;
		for (int i = 0; i < ARMOR.length; i++) {
			ItemStack stack = player.getItemBySlot(ARMOR[i]);
			if (stack.isEmpty() && preview) {
				stack = PREVIEW[i];
			}
			if (!stack.isEmpty() || c.armorShowEmpty) {
				entries.add(new Entry(ARMOR[i], stack));
			}
		}
		if (c.armorShowHands) {
			ItemStack main = player.getMainHandItem();
			ItemStack off = player.getOffhandItem();
			if (preview && !main.isDamageableItem()) {
				main = PREVIEW[4];
			}
			if (preview && !off.isDamageableItem()) {
				off = PREVIEW[5];
			}
			// Hände nur zeigen, wenn dort etwas mit Haltbarkeit liegt (Werkzeug, Waffe, Schild ...)
			if (main.isDamageableItem()) {
				entries.add(new Entry(EquipmentSlot.MAINHAND, main));
			}
			if (off.isDamageableItem()) {
				entries.add(new Entry(EquipmentSlot.OFFHAND, off));
			}
		}
		return entries;
	}

	private static void extractCell(GuiGraphicsExtractor graphics, Font font, int x, int y, Entry entry, HudElementConfig ec, LuConfig c) {
		int size = ArmorHudLayout.SLOT;
		// Slot im Theme-Stil
		graphics.fill(x, y, x + size, y + size, ec.outlineColor);
		graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, ARGB.color(200, c.colorSlot & 0xFFFFFF));
		graphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, ARGB.color(200, c.colorHighlight & 0xFFFFFF));
		graphics.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, ARGB.color(200, c.colorHighlight & 0xFFFFFF));

		ItemStack stack = entry.stack();
		if (stack.isEmpty()) {
			return;
		}
		graphics.item(stack, x + 1, y + 1);

		int barY = y + size;
		boolean damageable = stack.isDamageableItem();
		float fraction = damageable ? 1f - stack.getDamageValue() / (float) stack.getMaxDamage() : 1f;
		if (c.armorDurability.bar() && damageable) {
			extractBar(graphics, x, barY + 1, size, fraction, ec, stack);
		}
		if (c.armorDurability.text() && damageable) {
			int textY = barY + (c.armorDurability.bar() ? ArmorHudLayout.BAR : 0) + 1;
			String text = c.armorDurability == dev.lunaos.luhub.config.ArmorDurability.NUMBER
				? String.valueOf(stack.getMaxDamage() - stack.getDamageValue())
				: String.format(Locale.ROOT, "%d%%", Math.round(fraction * 100));
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(x + size / 2f, textY);
			pose.scale(0.5f, 0.5f);
			int color = c.armorDurability.bar() ? 0xFFE6E8FF : (0xFF000000 | stack.getBarColor());
			graphics.text(font, text, -font.width(text) / 2, 0, color, true);
			pose.popMatrix();
		}

		boolean low = damageable && fraction * 100 <= c.armorWarnPercent;
		if (low) {
			if ((Util.getMillis() / 400) % 2 == 0) {
				extractWarning(graphics, x + size - 5, y - 2);
			}
			if (c.armorWarnSound && !LuHud.previewMode && !WARNED.getOrDefault(entry.slot(), false)) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 0.6f));
			}
		}
		if (!LuHud.previewMode) {
			WARNED.put(entry.slot(), low);
		}
	}

	private static void extractBar(GuiGraphicsExtractor graphics, int x, int y, int width, float fraction, HudElementConfig ec, ItemStack stack) {
		graphics.fill(x, y, x + width, y + 2, ec.emptyColor);
		int filled = Math.round((width) * fraction);
		int[] stripes = ec.style.isPride() ? PrideFlags.stripes(ec.style) : null;
		for (int px = 0; px < filled; px++) {
			float t = px / (float) (width - 1);
			int color = switch (ec.style) {
				case VANILLA -> 0xFF000000 | stack.getBarColor();
				case NIGHT -> NightTheme.lerp(0xFFB49BFF, 0xFF8FF5FF, t);
				case CUSTOM -> ec.fillColor;
				default -> PrideFlags.smooth(stripes, t);
			};
			// knappe Haltbarkeit immer deutlich rot einfärben, egal welcher Stil
			if (ec.style != BarStyle.VANILLA && fraction < 0.25f) {
				color = NightTheme.lerp(color, 0xFFFF5A6A, 0.6f);
			}
			graphics.fill(x + px, y, x + px + 1, y + 2, color);
		}
	}

	/** Kleines rotes Ausrufezeichen. */
	private static void extractWarning(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x, y, x + 4, y + 7, 0xFF3A0610);
		graphics.fill(x + 1, y + 1, x + 3, y + 4, 0xFFFF4D5E);
		graphics.fill(x + 1, y + 5, x + 3, y + 6, 0xFFFF4D5E);
	}
}
