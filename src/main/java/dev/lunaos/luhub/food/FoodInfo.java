package dev.lunaos.luhub.food;

import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.hud.PixelIcon;
import dev.lunaos.luhub.mixin.FoodDataAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
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

	public static boolean active() {
		return !ExternalMods.APPLESKIN;
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

	/** Erschöpfung des Spielers – nur im Einzelspieler bekannt, sonst 0. */
	private static float exhaustion(Minecraft mc) {
		if (mc.player == null || mc.getSingleplayerServer() == null) {
			return 0f;
		}
		ServerPlayer serverPlayer = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
		return serverPlayer == null ? 0f : ((FoodDataAccessor) serverPlayer.getFoodData()).luhub$getExhaustionLevel();
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
		if (!active() || !c.foodHealthPreview || player == null) {
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
			boolean onlyRightHalf = now == 1;
			boolean half = then == 1;
			PixelIcon.HEART.draw(graphics, x, y, (ch, px, py) -> {
				if (ch == '.') {
					return 0;
				}
				if (ch == 'o') {
					return ARGB.color(alpha, 0x2A0618);
				}
				if ((half && px > 4) || (onlyRightHalf && px <= 4)) {
					return 0;
				}
				return ARGB.color(alpha, ch == 'h' ? 0xFFD0E8 : 0xFF7DB8);
			});
		}
	}

	/** Wird vor den Hunger-Keulen gezeichnet. */
	public static void underlay(GuiGraphicsExtractor graphics) {
		LuConfig c = LuConfig.get();
		Minecraft mc = Minecraft.getInstance();
		if (!active() || !c.foodExhaustionUnderlay || mc.player == null || mc.getSingleplayerServer() == null) {
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
		if (!active() || player == null) {
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
					boolean half = halvesNew == 1;
					PixelIcon.FOOD.draw(graphics, x, y, (ch, px, py) -> {
						if (ch == '.' || (half && px < 4 && ch != 'o')) {
							return 0;
						}
						return switch (ch) {
							case 'o' -> ARGB.color(alpha, 0x2A1A06);
							case 'w' -> ARGB.color(alpha, 0xF4F0FF);
							default -> ARGB.color(alpha, 0xFFC46B);
						};
					});
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
			boolean full = coveredTo >= 1.5f;
			int x = right - i * 8 - 9;
			PixelIcon.FOOD.draw(graphics, x, y, (ch, px, py) -> ch == 'o' && (full || px >= 4) ? color : 0);
		}
	}
}
