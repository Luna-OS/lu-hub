package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.mixin.HudAccessor;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Zeichnet Herzen, Hunger, Luftblasen und die XP-Leiste im gewählten Stil
 * (Nachthimmel, eigene Farben oder Pride-Flagge). Die Logik folgt der Vanilla-HUD,
 * damit sich alles gewohnt verhält (Wackeln bei wenig Leben, Regeneration, Absorption ...).
 */
public final class StatusBars {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final int POISON = 0xFF8DB82D;
	private static final int WITHER = 0xFF3B3B44;
	private static final int FROZEN = 0xFFA6EBFF;
	private static final int ABSORPTION = 0xFFFFD84A;
	private static final int HUNGER_EFFECT = 0xFF9CBF4A;

	private static int lastHealth;
	private static long blinkUntilTick;

	private StatusBars() {
	}

	private static Player player() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getCameraEntity() instanceof Player p ? p : null;
	}

	// ------------------------------------------------------------------ Herzen

	public static boolean health(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Player player = player();
		if (player == null) {
			return true;
		}
		int tick = player.tickCount;
		int health = Mth.ceil(player.getHealth());
		if (health < lastHealth && player.damageCooldownTime > 0) {
			blinkUntilTick = tick + 20;
		} else if (health > lastHealth && player.damageCooldownTime > 0) {
			blinkUntilTick = tick + 10;
		}
		lastHealth = health;
		boolean blink = blinkUntilTick > tick && (blinkUntilTick - tick) / 3L % 2L == 1L;

		float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), health);
		int absorption = Mth.ceil(player.getAbsorptionAmount());
		int containers = Mth.ceil(maxHealth / 2.0);
		int absorbContainers = Mth.ceil(absorption / 2.0);
		int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		int regenIndex = player.hasEffect(MobEffects.REGENERATION) ? tick % Mth.ceil(maxHealth + 5.0F) : -1;

		int statusColor = 0;
		if (player.hasEffect(MobEffects.POISON)) {
			statusColor = POISON;
		} else if (player.hasEffect(MobEffects.WITHER)) {
			statusColor = WITHER;
		} else if (player.isFullyFrozen()) {
			statusColor = FROZEN;
		}
		boolean hardcore = player.level().getLevelData().isHardcore();

		int baseX = HudElementType.HEALTH.defaultX(graphics.guiWidth());
		int baseY = HudElementType.HEALTH.defaultY(graphics.guiHeight());
		RANDOM.setSeed(tick * 312871L);

		for (int i = containers + absorbContainers - 1; i >= 0; i--) {
			int x = baseX + (i % 10) * 8;
			int y = baseY - (i / 10) * rowHeight;
			if (health + absorption <= 4) {
				y += RANDOM.nextInt(2);
			}
			if (i < containers && i == regenIndex) {
				y -= 2;
			}
			int halves = i * 2;
			boolean isAbsorption = i >= containers;
			int fillHalves;
			int fixedColor = statusColor;
			if (isAbsorption) {
				int absHalves = halves - containers * 2;
				fillHalves = Math.clamp(absorption - absHalves, 0, 2);
				if (statusColor != WITHER) {
					fixedColor = ABSORPTION;
				}
			} else {
				fillHalves = Math.clamp(health - halves, 0, 2);
			}
			drawIcon(graphics, PixelIcon.HEART, x, y, HudElementType.HEALTH, ec, i, fillHalves, true, fixedColor, blink && !isAbsorption, hardcore);
		}
		return true;
	}

	// ------------------------------------------------------------------ Rüstung

	public static boolean armor(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Player player = player();
		if (player == null) {
			return true;
		}
		int armor = player.getArmorValue();
		if (armor <= 0 && !LuHud.previewMode) {
			return true;
		}
		if (armor <= 0) {
			armor = 13; // Vorschau im HUD-Editor
		}
		// wie Vanilla: die Rüstungsleiste rutscht mit jeder weiteren Herzreihe nach oben
		float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), Mth.ceil(player.getHealth()));
		int absorption = Mth.ceil(player.getAbsorptionAmount());
		int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		int baseX = HudElementType.ARMOR.defaultX(graphics.guiWidth());
		int y = HudElementType.ARMOR.defaultY(graphics.guiHeight()) - (rows - 1) * rowHeight;
		for (int i = 0; i < 10; i++) {
			int fill = Math.clamp(armor - i * 2, 0, 2);
			drawIcon(graphics, PixelIcon.ARMOR, baseX + i * 8, y, HudElementType.ARMOR, ec, i, fill, true, 0, false, false);
		}
		return true;
	}

	// ------------------------------------------------------------------ Hunger

	public static boolean food(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Player player = player();
		if (player == null) {
			return true;
		}
		int food = player.getFoodData().getFoodLevel();
		boolean hungry = player.hasEffect(MobEffects.HUNGER);
		int baseRight = HudElementType.FOOD.defaultX(graphics.guiWidth()) + 81;
		int baseY = HudElementType.FOOD.defaultY(graphics.guiHeight());
		int tick = player.tickCount;
		RANDOM.setSeed(tick * 312871L + 7);
		for (int i = 0; i < 10; i++) {
			int y = baseY;
			if (player.getFoodData().getSaturationLevel() <= 0.0F && tick % (food * 3 + 1) == 0) {
				y += RANDOM.nextInt(3) - 1;
			}
			int x = baseRight - i * 8 - 9;
			int fill = Math.clamp(food - i * 2, 0, 2);
			drawIcon(graphics, PixelIcon.FOOD, x, y, HudElementType.FOOD, ec, i, fill, false, hungry ? HUNGER_EFFECT : 0, false, false);
		}
		return true;
	}

	// ------------------------------------------------------------------ Luft

	public static boolean air(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Player player = player();
		if (player == null) {
			return true;
		}
		int max = player.getMaxAirSupply();
		int air = Math.clamp(player.getAirSupply(), 0, max);
		boolean underWater = player.isEyeInFluid(FluidTags.WATER);
		boolean preview = LuHud.previewMode;
		if (!preview && !underWater && air >= max) {
			return true;
		}
		int full = preview && air >= max ? 10 : Mth.ceil((air - 2) * 10f / max);
		int popping = Mth.ceil(air * 10f / max);
		int baseRight = HudElementType.AIR.defaultX(graphics.guiWidth()) + 81;
		int baseY = HudElementType.AIR.defaultY(graphics.guiHeight());
		for (int bubble = 1; bubble <= 10; bubble++) {
			int x = baseRight - (bubble - 1) * 8 - 9;
			if (bubble <= full) {
				drawIcon(graphics, PixelIcon.BUBBLE, x, baseY, HudElementType.AIR, ec, bubble, 2, false, 0, false, false);
			} else if (bubble == popping && full != popping && underWater) {
				drawBubbleOutline(graphics, x, baseY, ARGB.color(255, 0xFFFFFF));
			} else {
				drawBubbleOutline(graphics, x, baseY, ARGB.color(70, ec.outlineColor & 0xFFFFFF));
			}
		}
		return true;
	}

	private static void drawBubbleOutline(GuiGraphicsExtractor graphics, int x, int y, int color) {
		PixelIcon.BUBBLE.draw(graphics, x, y, (ch, px, py) -> ch == 'o' ? color : 0);
	}

	// ------------------------------------------------------------------ XP

	public static boolean xpBar(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(((HudAccessor) mc.gui.hud).luhub$getContextualInfoBar().getSecond() instanceof ExperienceBar)) {
			return false; // Ortungs- oder Sprungleiste: Vanilla zeichnen lassen
		}
		if (mc.player.getXpNeededForNextLevel() <= 0) {
			return true;
		}
		int x = HudElementType.XP_BAR.defaultX(graphics.guiWidth());
		int y = HudElementType.XP_BAR.defaultY(graphics.guiHeight());
		int w = HudElementType.XP_BAR.width();
		int h = HudElementType.XP_BAR.height();
		int progress = LuHud.previewMode && mc.player.experienceProgress <= 0 ? w * 2 / 3 : (int) (mc.player.experienceProgress * (w - 2));

		graphics.fill(x, y, x + w, y + h, ec.outlineColor);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, ec.emptyColor);
		int[] stripes = ec.style.isPride() ? PrideFlags.stripes(ec.style) : null;
		double time = Util.getMillis() / 1000.0;
		for (int px = 0; px < progress; px++) {
			float t = px / (float) (w - 2);
			int top;
			int bottom;
			switch (ec.style) {
				case NIGHT -> {
					top = NightTheme.lerp(0xFFB49BFF, 0xFF8FF5FF, t);
					bottom = NightTheme.lerp(0xFF6B4FD6, 0xFF3FB6D9, t);
				}
				case CUSTOM -> {
					top = ec.fillColor;
					bottom = darken(ec.fillColor, 0.3f);
				}
				default -> {
					top = PrideFlags.smooth(stripes, t);
					bottom = darken(top, 0.18f);
				}
			}
			graphics.fill(x + 1 + px, y + 1, x + 2 + px, y + 2, NightTheme.lerp(top, 0xFFFFFFFF, 0.25f));
			graphics.fill(x + 1 + px, y + 2, x + 2 + px, y + h - 2, top);
			graphics.fill(x + 1 + px, y + h - 2, x + 2 + px, y + h - 1, bottom);
		}
		if (ec.sparkle && progress > 4) {
			// zwei kleine Sterne wandern über die gefüllte Leiste
			for (int s = 0; s < 2; s++) {
				int sx = x + 1 + (int) (((time * 22 + s * 61) % progress));
				float twinkle = (float) (0.5 + 0.5 * Math.sin(time * 5 + s * 2));
				graphics.fill(sx, y + 2, sx + 1, y + 3, ARGB.color((int) (120 + 135 * twinkle), 0xFFFFFF));
			}
		}
		return true;
	}

	public static boolean xpLevel(GuiGraphicsExtractor graphics, DeltaTracker delta, HudElementConfig ec) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return true;
		}
		Font font = mc.font;
		String text = Component.translatable("gui.experience.level", mc.player.experienceLevel).getString();
		int width = font.width(text);
		int x = (graphics.guiWidth() - width) / 2;
		int y = graphics.guiHeight() - 24 - 9 - 2;
		int outline = ec.outlineColor | 0xFF000000;
		graphics.text(font, text, x + 1, y, outline, false);
		graphics.text(font, text, x - 1, y, outline, false);
		graphics.text(font, text, x, y + 1, outline, false);
		graphics.text(font, text, x, y - 1, outline, false);
		int[] stripes = ec.style.isPride() ? PrideFlags.stripes(ec.style) : null;
		int cx = x;
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			float t = text.length() <= 1 ? 0.5f : i / (float) (text.length() - 1);
			int color = switch (ec.style) {
				case NIGHT -> NightTheme.lerp(0xFFB49BFF, 0xFF9DF5FF, t);
				case CUSTOM -> ec.fillColor;
				default -> PrideFlags.at(stripes, t);
			};
			graphics.text(font, ch, cx, y, color | 0xFF000000, false);
			cx += font.width(ch);
		}
		return true;
	}

	// ------------------------------------------------------------------ Symbol zeichnen

	/**
	 * @param fillHalves  0 = leer, 1 = halb, 2 = voll
	 * @param leftHalf    ob ein halbes Symbol die linke (Herz) oder rechte Hälfte (Keule) füllt
	 * @param fixedColor  Status-Farbe (Gift, Wither, Absorption ...) oder 0
	 */
	private static void drawIcon(GuiGraphicsExtractor graphics, PixelIcon icon, int x, int y, HudElementType type,
								 HudElementConfig ec, int index, int fillHalves, boolean leftHalf, int fixedColor,
								 boolean blink, boolean hardcore) {
		BarStyle style = ec.style;
		int[] stripes = style.isPride() ? PrideFlags.stripes(style) : null;
		double time = Util.getMillis() / 1000.0;
		boolean twinkle = ec.sparkle && Math.sin(time * 2.2 + NightTheme.hash(index, 9, type.ordinal()) * 40) > 0.93;
		int outline = blink ? NightTheme.lerp(ec.outlineColor, 0xFFFFFFFF, 0.7f) : ec.outlineColor;
		int half = icon.width / 2;

		icon.draw(graphics, x, y, (ch, px, py) -> {
			if (ch == '.') {
				return 0;
			}
			if (ch == 'o') {
				return hardcore && py == 0 ? NightTheme.lerp(outline, 0xFFFFD84A, 0.6f) : outline;
			}
			boolean filled = fillHalves == 2 || (fillHalves == 1 && (leftHalf ? px <= half : px >= half));
			if (!filled) {
				return ec.emptyColor;
			}
			if (ch == 'w') {
				return 0xFFF4F0FF;
			}
			float t = icon.rowT(py);
			int color;
			if (fixedColor != 0) {
				color = NightTheme.lerp(NightTheme.lerp(fixedColor, 0xFFFFFFFF, 0.25f), darken(fixedColor, 0.25f), t);
			} else {
				color = switch (style) {
					case NIGHT -> nightColor(type, t);
					case CUSTOM -> NightTheme.lerp(NightTheme.lerp(ec.fillColor, 0xFFFFFFFF, 0.15f), darken(ec.fillColor, 0.3f), t);
					default -> PrideFlags.at(stripes, t);
				};
			}
			if (ch == 'h') {
				return twinkle ? 0xFFFFFFFF : NightTheme.lerp(color, 0xFFFFFFFF, style.isPride() ? 0.25f : 0.55f);
			}
			return color;
		});
	}

	private static int nightColor(HudElementType type, float t) {
		return switch (type) {
			case HEALTH -> NightTheme.lerp(0xFFFF9BD2, 0xFFC2367F, t);
			case FOOD -> NightTheme.lerp(0xFFFFE9A8, 0xFFE0963A, t);
			case AIR -> NightTheme.lerp(0xE6D8F8FF, 0xE66AB8E6, t);
			case ARMOR -> NightTheme.lerp(0xFFF0F2FF, 0xFF8A93D6, t);
			default -> NightTheme.lerp(0xFFB49BFF, 0xFF6B4FD6, t);
		};
	}

	public static int darken(int color, float amount) {
		return NightTheme.lerp(color, color & 0xFF000000, amount);
	}
}
