package dev.lunaos.luhub.food;

import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.hud.PixelIcon;
import dev.lunaos.luhub.mixin.FoodDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Hunger-Infos wie bei AppleSkin, direkt in Lu Hub: Sättigung als goldener Rand um die Keulen,
 * Erschöpfung als Balken hinter der Hungerleiste (Einzelspieler) und eine blinkende Vorschau,
 * wie viel Hunger, Sättigung und Leben das gehaltene Essen bringt.
 */
public final class FoodInfo {
	private static final int SATURATION = 0xFFFFD94A;
	private static final int EXHAUSTION = 0x60FFFFFF;

	private FoodInfo() {
	}

	/** Tooltip-Werte: nur ohne AppleSkin (sonst zeigt AppleSkin sie selbst). */
	public static boolean active() {
		return !ExternalMods.APPLESKIN;
	}

	/**
	 * HUD-Anzeigen auf einer Leiste. AppleSkin kann nur auf die Vanilla-Leisten zeichnen – sobald ein Lu-Hub-Stil
	 * (Nachthimmel, Pride, eigene Farben) aktiv ist, übernimmt Lu Hub auch bei installiertem AppleSkin.
	 */
	public static boolean activeFor(HudElementType type) {
		if (!ExternalMods.APPLESKIN) {
			return true;
		}
		LuConfig c = LuConfig.get();
		return c.hudEnabled && c.hud(type).style != BarStyle.VANILLA;
	}

	/** Gehaltenes Essen (Haupt- oder Zweithand) oder null. */
	public static FoodProperties heldFood(Player player) {
		for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food != null) {
				return food;
			}
		}
		return null;
	}

	/**
	 * Erschöpfung des Spielers. Sie wird normalerweise nicht an Clients geschickt: im Einzelspieler lesen wir sie
	 * direkt vom Server, mit AppleSkin auf dem Server synchronisiert AppleSkin sie in die Client-Daten. Sonst 0.
	 */
	private static float exhaustion(Minecraft mc) {
		if (mc.player == null) {
			return 0f;
		}
		if (mc.getSingleplayerServer() != null) {
			ServerPlayer serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
			return serverPlayer == null ? 0f : ((FoodDataAccessor) serverPlayer.getFoodData()).luhub$getExhaustionLevel();
		}
		return ExternalMods.APPLESKIN ? ((FoodDataAccessor) mc.player.getFoodData()).luhub$getExhaustionLevel() : 0f;
	}

	private static boolean exhaustionKnown(Minecraft mc) {
		return mc.getSingleplayerServer() != null || ExternalMods.APPLESKIN;
	}

	/**
	 * Wie viel Leben man durch natürliche Regeneration zurückbekommt, wenn man das Essen jetzt isst.
	 * Spielt die Vanilla-Regeln nach: volle Hungerleiste + Sättigung heilt schnell, ab 18 Hunger langsam,
	 * jede Heilung verbraucht Sättigung bzw. Hunger.
	 */
	public static float estimatedHeal(Player player, FoodProperties food) {
		float max = player.getMaxHealth();
		float health = player.getHealth();
		int foodLevel = Math.min(20, player.getFoodData().getFoodLevel() + food.nutrition());
		float saturation = Math.min(foodLevel, player.getFoodData().getSaturationLevel() + food.saturation());
		float exhaustion = exhaustion(Minecraft.getInstance());
		boolean peaceful = player.level().getDifficulty() == Difficulty.PEACEFUL;
		float start = health;
		for (int guard = 0; health < max && guard < 2000; guard++) {
			if (saturation > 0 && foodLevel >= 20) {
				float spent = Math.min(saturation, 6f);
				health += spent / 6f;
				exhaustion += spent;
			} else if (foodLevel >= 18) {
				health += 1f;
				exhaustion += 6f;
			} else {
				break;
			}
			while (exhaustion > 4f) {
				exhaustion -= 4f;
				if (saturation > 0) {
					saturation = Math.max(saturation - 1f, 0f);
				} else if (!peaceful) {
					foodLevel = Math.max(foodLevel - 1, 0);
				}
			}
		}
		return Math.min(health, max) - start;
	}

	/** Blinkende Herzen: so viel Leben würde das gehaltene Essen zurückbringen. */
	public static void healthOverlay(GuiGraphicsExtractor graphics) {
		LuConfig c = LuConfig.get();
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (!activeFor(HudElementType.HEALTH) || !c.foodHealthPreview || player == null) {
			return;
		}
		FoodProperties held = heldFood(player);
		if (held == null || !player.canEat(held.canAlwaysEat())) {
			return;
		}
		float gain = estimatedHeal(player, held);
		if (gain < 0.5f) {
			return;
		}
		float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
		int absorption = Mth.ceil(player.getAbsorptionAmount());
		int rows = Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F);
		int rowHeight = Math.max(10 - (rows - 2), 3);
		int current = Mth.ceil(player.getHealth());
		int future = Math.min(Mth.ceil(player.getHealth() + gain), Mth.ceil(maxHealth));
		int containers = Mth.ceil(maxHealth / 2.0);
		int baseX = HudElementType.HEALTH.defaultX(graphics.guiWidth());
		int baseY = HudElementType.HEALTH.defaultY(graphics.guiHeight());
		int alpha = Math.round((float) (0.35 + 0.45 * Math.abs(Math.sin(Util.getMillis() / 300.0))) * 255);
		for (int i = 0; i < containers; i++) {
			int now = Math.clamp(current - i * 2, 0, 2);
			int then = Math.clamp(future - i * 2, 0, 2);
			if (then <= now) {
				continue;
			}
			int x = baseX + (i % 10) * 8;
			int y = baseY - (i / 10) * rowHeight;
			PixelIcon.HEART.blit(graphics, healPreviewHeart(now == 1, then == 1), x, y, ARGB.color(alpha, 0xFFFFFF));
		}
	}

	/**
	 * Herz der Heil-Vorschau, deckend gebacken – die pulsierende Deckkraft kommt beim Zeichnen dazu.
	 * @param onlyRightHalf das Herz ist schon halb voll, nur die rechte Hälfte kommt dazu
	 * @param half          nach dem Essen nur halb voll
	 */
	private static Identifier healPreviewHeart(boolean onlyRightHalf, boolean half) {
		int index = (onlyRightHalf ? 2 : 0) + (half ? 1 : 0);
		return PixelIcon.HEART.fixed(HEAL_KEYS[index], HEAL_PAINTERS[index]);
	}

	private static final String[] HEAL_KEYS = {"heal_full", "heal_half", "heal_right", "heal_right_half"};
	private static final PixelIcon.Painter[] HEAL_PAINTERS = {
		healPainter(false, false), healPainter(false, true), healPainter(true, false), healPainter(true, true)
	};

	private static PixelIcon.Painter healPainter(boolean onlyRightHalf, boolean half) {
		return (ch, px, py) -> {
			if (ch == '.') {
				return 0;
			}
			if (ch == 'o') {
				return 0xFF2A0618;
			}
			if ((half && px > 4) || (onlyRightHalf && px <= 4)) {
				return 0;
			}
			return ch == 'h' ? 0xFFFFD0E8 : 0xFFFF7DB8;
		};
	}

	/** Keule der Essens-Vorschau (deckend), optional ohne die linke Hälfte. */
	private static int previewFood(char ch, boolean skip) {
		if (ch == '.' || (skip && ch != 'o')) {
			return 0;
		}
		return switch (ch) {
			case 'o' -> 0xFF2A1A06;
			case 'w' -> 0xFFF4F0FF;
			default -> 0xFFFFC46B;
		};
	}

	/** Wird vor den Hunger-Keulen gezeichnet. */
	public static void underlay(GuiGraphicsExtractor graphics) {
		LuConfig c = LuConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (!activeFor(HudElementType.FOOD) || !c.foodExhaustionUnderlay || mc.player == null || !exhaustionKnown(mc)) {
			return;
		}
		// Erschöpfung wird nicht an Clients geschickt – im Einzelspieler lesen wir sie direkt vom Server
		float exhaustion = exhaustion(mc);
		int width = Math.round(81 * Math.clamp(exhaustion / 4f, 0f, 1f));
		int right = HudElementType.FOOD.defaultX(graphics.guiWidth()) + 81;
		int y = HudElementType.FOOD.defaultY(graphics.guiHeight());
		if (width > 0) {
			graphics.fill(right - width, y, right, y + 9, EXHAUSTION);
		}
	}

	/** Wird nach den Hunger-Keulen gezeichnet. */
	public static void overlay(GuiGraphicsExtractor graphics) {
		LuConfig c = LuConfig.get();
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (!activeFor(HudElementType.FOOD) || player == null) {
			return;
		}
		int right = HudElementType.FOOD.defaultX(graphics.guiWidth()) + 81;
		int y = HudElementType.FOOD.defaultY(graphics.guiHeight());
		int foodLevel = player.getFoodData().getFoodLevel();
		float saturation = player.getFoodData().getSaturationLevel();

		FoodProperties held = c.foodHeldPreview ? heldFood(player) : null;
		boolean preview = held != null && player.canEat(held.canAlwaysEat());
		float flash = (float) (0.35 + 0.45 * Math.abs(Math.sin(Util.getMillis() / 300.0)));

		if (preview) {
			int newFood = Math.min(20, foodLevel + held.nutrition());
			for (int i = 0; i < 10; i++) {
				int halvesNow = Math.clamp(foodLevel - i * 2, 0, 2);
				int halvesNew = Math.clamp(newFood - i * 2, 0, 2);
				if (halvesNew > halvesNow) {
					int x = right - i * 8 - 9;
					int alpha = Math.round(flash * 255);
					Identifier icon = halvesNew == 1
						? PixelIcon.FOOD.fixed("preview_half", (ch, px, py) -> previewFood(ch, px < 4))
						: PixelIcon.FOOD.fixed("preview_full", (ch, px, py) -> previewFood(ch, false));
					PixelIcon.FOOD.blit(graphics, icon, x, y, ARGB.color(alpha, 0xFFFFFF));
				}
			}
		}

		if (c.foodSaturationOverlay) {
			drawSaturation(graphics, right, y, saturation, 255);
			if (preview) {
				float newFood = Math.min(20, foodLevel + held.nutrition());
				float newSaturation = Math.min(newFood, saturation + held.saturation());
				if (newSaturation > saturation) {
					drawSaturationRange(graphics, right, y, saturation, newSaturation, Math.round(flash * 255));
				}
			}
		}
	}

	private static void drawSaturation(GuiGraphicsExtractor graphics, int right, int y, float saturation, int alpha) {
		drawSaturationRange(graphics, right, y, 0, saturation, alpha);
	}

	/** Goldener Rand für den Sättigungsbereich [from, to) – voll, halb oder gar nicht pro Keule. */
	private static void drawSaturationRange(GuiGraphicsExtractor graphics, int right, int y, float from, float to, int alpha) {
		int color = ARGB.color(alpha, SATURATION & 0xFFFFFF);
		for (int i = 0; i < 10; i++) {
			float start = i * 2;
			float coveredTo = Math.clamp(to - start, 0f, 2f);
			float coveredFrom = Math.clamp(from - start, 0f, 2f);
			if (coveredTo <= coveredFrom || coveredTo < 0.5f) {
				continue;
			}
			int x = right - i * 8 - 9;
			Identifier icon = coveredTo >= 1.5f
				? PixelIcon.FOOD.fixed("saturation_full", (ch, px, py) -> ch == 'o' ? 0xFFFFFFFF : 0)
				: PixelIcon.FOOD.fixed("saturation_half", (ch, px, py) -> ch == 'o' && px >= 4 ? 0xFFFFFFFF : 0);
			// weißer Umriss, eingefärbt mit Sättigungsfarbe und Deckkraft
			PixelIcon.FOOD.blit(graphics, icon, x, y, color);
		}
	}
}
