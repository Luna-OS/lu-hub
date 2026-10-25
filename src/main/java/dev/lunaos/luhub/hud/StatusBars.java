package dev.lunaos.luhub.hud;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementConfig;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.mixin.HudAccessor;
import dev.lunaos.luhub.render.BakedTextures;
import dev.lunaos.luhub.theme.NightTheme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * <p>
 * Jedes Symbol ist eine gebackene Textur (siehe {@link Variants}) – pro Herz, Keule, Blase und Rüstungsteil
 * entsteht also genau ein GUI-Element. Die XP-Leiste besteht aus Rahmen, Hintergrund und einem
 * (abgeschnittenen) Verlauf als Textur.
 */
public final class StatusBars {
	private static final RandomSource RANDOM = RandomSource.create();
	private static final int POISON = 0xFF8DB82D;
	private static final int WITHER = 0xFF3B3B44;
	private static final int FROZEN = 0xFFA6EBFF;
	private static final int ABSORPTION = 0xFFFFD84A;
	private static final int HUNGER_EFFECT = 0xFF9CBF4A;
	/** Alle Status-Farben, die ein Symbol haben kann (0 = keine). Der Index geht in den Varianten-Schlüssel ein. */
	private static final int[] STATUS_COLORS = {0, POISON, WITHER, FROZEN, ABSORPTION, HUNGER_EFFECT};

	private static final Variants HEARTS = new Variants(PixelIcon.HEART, HudElementType.HEALTH);
	private static final Variants ARMOR = new Variants(PixelIcon.ARMOR, HudElementType.ARMOR);
	private static final Variants FOOD = new Variants(PixelIcon.FOOD, HudElementType.FOOD);
	private static final Variants BUBBLES = new Variants(PixelIcon.BUBBLE, HudElementType.AIR);
	private static final XpBarTexture XP_BAR = new XpBarTexture();

	private static int lastHealth;
	private static long blinkUntilTick;

	/** XP-Stufe: Text und Breiten werden nur neu berechnet, wenn sich die Stufe ändert. */
	private static int cachedLevel = Integer.MIN_VALUE;
	private static String levelText = "";
	private static String[] levelChars = new String[0];
	private static int[] levelCharX = new int[0];
	private static int levelWidth;

	private StatusBars() {
	}

	private static Player player() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getCameraEntity() instanceof Player p ? p : null;
	}

	private static boolean twinkle(HudElementConfig ec, HudElementType type, int index, double time) {
		return ec.sparkle && Math.sin(time * 2.2 + NightTheme.hash(index, 9, type.ordinal()) * 40) > 0.93;
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
		double time = Util.getMillis() / 1000.0;
		HEARTS.update(ec);
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
			Identifier icon = HEARTS.get(fillHalves, true, fixedColor, blink && !isAbsorption, hardcore,
				twinkle(ec, HudElementType.HEALTH, i, time));
			PixelIcon.HEART.blit(graphics, icon, x, y);
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
		double time = Util.getMillis() / 1000.0;
		ARMOR.update(ec);
		for (int i = 0; i < 10; i++) {
			int fill = Math.clamp(armor - i * 2, 0, 2);
			Identifier icon = ARMOR.get(fill, true, 0, false, false, twinkle(ec, HudElementType.ARMOR, i, time));
			PixelIcon.ARMOR.blit(graphics, icon, baseX + i * 8, y);
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
		boolean starving = player.getFoodData().getSaturationLevel() <= 0.0F;
		int status = player.hasEffect(MobEffects.HUNGER) ? HUNGER_EFFECT : 0;
		int baseRight = HudElementType.FOOD.defaultX(graphics.guiWidth()) + 81;
		int baseY = HudElementType.FOOD.defaultY(graphics.guiHeight());
		int tick = player.tickCount;
		double time = Util.getMillis() / 1000.0;
		FOOD.update(ec);
		RANDOM.setSeed(tick * 312871L + 7);
		for (int i = 0; i < 10; i++) {
			int y = baseY;
			if (starving && tick % (food * 3 + 1) == 0) {
				y += RANDOM.nextInt(3) - 1;
			}
			int x = baseRight - i * 8 - 9;
			int fill = Math.clamp(food - i * 2, 0, 2);
			Identifier icon = FOOD.get(fill, false, status, false, false, twinkle(ec, HudElementType.FOOD, i, time));
			PixelIcon.FOOD.blit(graphics, icon, x, y);
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
		double time = Util.getMillis() / 1000.0;
		BUBBLES.update(ec);
		for (int bubble = 1; bubble <= 10; bubble++) {
			int x = baseRight - (bubble - 1) * 8 - 9;
			Identifier icon;
			if (bubble <= full) {
				icon = BUBBLES.get(2, false, 0, false, false, twinkle(ec, HudElementType.AIR, bubble, time));
			} else {
				icon = BUBBLES.outline(bubble == popping && full != popping && underWater);
			}
			PixelIcon.BUBBLE.blit(graphics, icon, x, baseY);
		}
		return true;
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
		if (progress > 0) {
			// der Verlauf liegt fertig als Textur vor – gezeigt wird nur der gefüllte Teil
			Identifier fill = XP_BAR.get(ec, w - 2, h - 2);
			graphics.blit(RenderPipelines.GUI_TEXTURED, fill, x + 1, y + 1, 0, 0, progress, h - 2, progress, h - 2, w - 2, h - 2);
		}
		if (ec.sparkle && progress > 4) {
			// zwei kleine Sterne wandern über die gefüllte Leiste
			double time = Util.getMillis() / 1000.0;
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
		updateLevelText(font, mc.player.experienceLevel);
		int x = (graphics.guiWidth() - levelWidth) / 2;
		int y = graphics.guiHeight() - 24 - 9 - 2;
		int outline = ec.outlineColor | 0xFF000000;
		graphics.text(font, levelText, x + 1, y, outline, false);
		graphics.text(font, levelText, x - 1, y, outline, false);
		graphics.text(font, levelText, x, y + 1, outline, false);
		graphics.text(font, levelText, x, y - 1, outline, false);
		int[] stripes = ec.style.isPride() ? PrideFlags.stripes(ec.style) : null;
		int length = levelChars.length;
		for (int i = 0; i < length; i++) {
			float t = length <= 1 ? 0.5f : i / (float) (length - 1);
			int color = switch (ec.style) {
				case NIGHT -> NightTheme.lerp(0xFFB49BFF, 0xFF9DF5FF, t);
				case CUSTOM -> ec.fillColor;
				default -> PrideFlags.at(stripes, t);
			};
			graphics.text(font, levelChars[i], x + levelCharX[i], y, color | 0xFF000000, false);
		}
		return true;
	}

	private static void updateLevelText(Font font, int level) {
		if (level == cachedLevel) {
			return;
		}
		cachedLevel = level;
		levelText = Component.translatable("gui.experience.level", level).getString();
		levelWidth = font.width(levelText);
		levelChars = new String[levelText.length()];
		levelCharX = new int[levelText.length()];
		int cx = 0;
		for (int i = 0; i < levelChars.length; i++) {
			levelChars[i] = String.valueOf(levelText.charAt(i));
			levelCharX[i] = cx;
			cx += font.width(levelChars[i]);
		}
	}

	// ------------------------------------------------------------------ Farben

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

	private static int statusIndex(int color) {
		for (int i = 0; i < STATUS_COLORS.length; i++) {
			if (STATUS_COLORS[i] == color) {
				return i;
			}
		}
		return 0;
	}

	// ------------------------------------------------------------------ gebackene Symbole

	/**
	 * Alle Varianten eines Symbols für die aktuellen Einstellungen eines HUD-Elements.
	 * Varianten werden beim ersten Gebrauch gebacken; ändern sich Stil oder Farben, werden alle verworfen.
	 * Die Skalierung braucht keine neuen Texturen – sie wirkt über die Pose beim Zeichnen.
	 */
	private static final class Variants {
		private static final int FILLS = 3;
		private static final int SLOTS = FILLS * 2 * STATUS_COLORS.length * 2 * 2 * 2;

		private final PixelIcon icon;
		private final HudElementType type;
		private final Identifier[] baked = new Identifier[SLOTS];
		private Identifier outlinePopping;
		private Identifier outlineDim;
		private boolean valid;
		private BarStyle style;
		private int fillColor;
		private int emptyColor;
		private int outlineColor;

		Variants(PixelIcon icon, HudElementType type) {
			this.icon = icon;
			this.type = type;
		}

		/** Einmal pro Frame: Einstellungen vergleichen und bei Änderungen alle Varianten verwerfen. */
		void update(HudElementConfig ec) {
			if (valid && ec.style == style && ec.fillColor == fillColor && ec.emptyColor == emptyColor && ec.outlineColor == outlineColor) {
				return;
			}
			for (int i = 0; i < baked.length; i++) {
				BakedTextures.release(baked[i]);
				baked[i] = null;
			}
			BakedTextures.release(outlinePopping);
			BakedTextures.release(outlineDim);
			outlinePopping = null;
			outlineDim = null;
			valid = true;
			style = ec.style;
			fillColor = ec.fillColor;
			emptyColor = ec.emptyColor;
			outlineColor = ec.outlineColor;
		}

		/**
		 * @param fillHalves 0 = leer, 1 = halb, 2 = voll
		 * @param leftHalf   ob ein halbes Symbol die linke (Herz) oder rechte Hälfte (Keule) füllt
		 * @param fixedColor Status-Farbe (Gift, Wither, Absorption ...) oder 0
		 */
		Identifier get(int fillHalves, boolean leftHalf, int fixedColor, boolean blink, boolean hardcore, boolean twinkle) {
			int status = statusIndex(fixedColor);
			int index = ((((fillHalves * 2 + (leftHalf ? 1 : 0)) * STATUS_COLORS.length + status) * 2 + (blink ? 1 : 0)) * 2
				+ (hardcore ? 1 : 0)) * 2 + (twinkle ? 1 : 0);
			Identifier id = baked[index];
			if (id == null) {
				id = bake(fillHalves, leftHalf, STATUS_COLORS[status], blink, hardcore, twinkle);
				baked[index] = id;
			}
			return id;
		}

		/** Nur der Umriss (leere Luftblasen): weiß beim Platzen, sonst blass in der Umrissfarbe. */
		Identifier outline(boolean popping) {
			if (popping) {
				if (outlinePopping == null) {
					int color = ARGB.color(255, 0xFFFFFF);
					outlinePopping = icon.bake((ch, px, py) -> ch == 'o' ? color : 0);
				}
				return outlinePopping;
			}
			if (outlineDim == null) {
				int color = ARGB.color(70, outlineColor & 0xFFFFFF);
				outlineDim = icon.bake((ch, px, py) -> ch == 'o' ? color : 0);
			}
			return outlineDim;
		}

		private Identifier bake(int fillHalves, boolean leftHalf, int fixedColor, boolean blink, boolean hardcore, boolean twinkle) {
			int[] stripes = style.isPride() ? PrideFlags.stripes(style) : null;
			int outline = blink ? NightTheme.lerp(outlineColor, 0xFFFFFFFF, 0.7f) : outlineColor;
			int half = icon.width / 2;
			return icon.bake((ch, px, py) -> {
				if (ch == '.') {
					return 0;
				}
				if (ch == 'o') {
					return hardcore && py == 0 ? NightTheme.lerp(outline, 0xFFFFD84A, 0.6f) : outline;
				}
				boolean filled = fillHalves == 2 || (fillHalves == 1 && (leftHalf ? px <= half : px >= half));
				if (!filled) {
					return emptyColor;
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
						case CUSTOM -> NightTheme.lerp(NightTheme.lerp(fillColor, 0xFFFFFFFF, 0.15f), darken(fillColor, 0.3f), t);
						default -> PrideFlags.at(stripes, t);
					};
				}
				if (ch == 'h') {
					return twinkle ? 0xFFFFFFFF : NightTheme.lerp(color, 0xFFFFFFFF, style.isPride() ? 0.25f : 0.55f);
				}
				return color;
			});
		}
	}

	/** Der volle Verlauf der XP-Leiste als Textur; neu gebacken, wenn sich Stil oder Füllfarbe ändern. */
	private static final class XpBarTexture {
		private Identifier texture;
		private BarStyle style;
		private int fillColor;
		private int width;
		private int height;

		Identifier get(HudElementConfig ec, int w, int h) {
			if (texture != null && ec.style == style && ec.fillColor == fillColor && w == width && h == height) {
				return texture;
			}
			BakedTextures.release(texture);
			style = ec.style;
			fillColor = ec.fillColor;
			width = w;
			height = h;
			int[] stripes = style.isPride() ? PrideFlags.stripes(style) : null;
			int[] tops = new int[w];
			int[] bottoms = new int[w];
			for (int px = 0; px < w; px++) {
				float t = px / (float) w;
				switch (style) {
					case NIGHT -> {
						tops[px] = NightTheme.lerp(0xFFB49BFF, 0xFF8FF5FF, t);
						bottoms[px] = NightTheme.lerp(0xFF6B4FD6, 0xFF3FB6D9, t);
					}
					case CUSTOM -> {
						tops[px] = fillColor;
						bottoms[px] = darken(fillColor, 0.3f);
					}
					default -> {
						tops[px] = PrideFlags.smooth(stripes, t);
						bottoms[px] = darken(tops[px], 0.18f);
					}
				}
			}
			// oberste Zeile aufgehellt, unterste Zeile dunkler – wie vorher die drei fill()-Aufrufe pro Spalte
			texture = BakedTextures.bake("xp_bar", w, h, (px, py) -> py == 0 ? NightTheme.lerp(tops[px], 0xFFFFFFFF, 0.25f)
				: py == h - 1 ? bottoms[px] : tops[px]);
			return texture;
		}
	}
}
