package dev.lunaos.luhub.test;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.compat.ShulkerBoxTooltipCompat;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.mixin.ButtonAccessor;
import dev.lunaos.luhub.mixin.ImageButtonAccessor;
import dev.lunaos.luhub.screen.ColorPickerScreen;
import dev.lunaos.luhub.sort.Sorter;
import dev.lunaos.luhub.screen.DebugEditorScreen;
import dev.lunaos.luhub.screen.HudEditorScreen;
import dev.lunaos.luhub.screen.LuConfigScreen;
import dev.lunaos.luhub.screen.TooltipEditorScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Startet eine Testwelt und macht Screenshots aller Lu-Hub-Features. */
public final class LuHubClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.waitTicks(40);
		context.takeScreenshot("luhub-00-title");
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set midnight");
			world.getServer().runCommand("give @a minecraft:diamond_sword");
			world.getServer().runCommand("give @a minecraft:golden_apple 5");
			world.getServer().runCommand("give @a minecraft:torch 32");
			world.getServer().runCommand("give @a minecraft:red_shulker_box[minecraft:container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}},"
				+ "{slot:4,item:{id:\"minecraft:cake\",count:1}},{slot:13,item:{id:\"minecraft:axolotl_bucket\",count:1}},{slot:26,item:{id:\"minecraft:torch\",count:64}}]]");
			world.getServer().runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");
			world.getServer().runCommand("item replace entity @a armor.chest with minecraft:iron_chestplate[minecraft:damage=200]");
			world.getServer().runCommand("item replace entity @a armor.legs with minecraft:golden_leggings[minecraft:damage=100]");
			world.getServer().runCommand("item replace entity @a armor.feet with minecraft:leather_boots[minecraft:damage=40]");
			world.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:golden_apple 3");
			world.getServer().runCommand("xp add @a 7 levels");
			world.getServer().runCommand("xp add @a 40 points");
			world.getServer().runCommand("damage @p 7");
			world.getServer().runCommand("effect give @a absorption 600 0 true");
			context.waitTicks(80);
			context.takeScreenshot("luhub-01-hud-night");
			world.getServer().runCommand("damage @p 9");
			context.waitTicks(4);
			context.takeScreenshot("luhub-01b-heal-preview");

			setStyle(context, BarStyle.TRANS);
			context.takeScreenshot("luhub-02-hud-trans");
			setStyle(context, BarStyle.RAINBOW);
			context.takeScreenshot("luhub-03-hud-rainbow");
			setStyle(context, BarStyle.BI);
			context.takeScreenshot("luhub-04-hud-bi");
			setStyle(context, BarStyle.NIGHT);

			context.getInput().pressKey(options -> options.keyDebugOverlay);
			context.waitTicks(10);
			context.takeScreenshot("luhub-05-f3");
			context.getInput().pressKey(options -> options.keyDebugOverlay);

			context.setScreen(() -> new InventoryScreen(context.computeOnClient(mc -> mc.player)));
			context.waitTicks(5);
			context.takeScreenshot("luhub-06-inventory");
			hoverHotbarSlot(context, 3);
			context.takeScreenshot("luhub-06b-shulker-tooltip");
			// Shulker Box Tooltip ohne "farbige Vorschau" übergibt Weiß – Lu Hub muss trotzdem Rot erkennen
			int themed = context.computeOnClient(mc -> ShulkerBoxTooltipCompat.tint(0xFFFFFFFF));
			int red = context.computeOnClient(mc -> ShulkerBoxTooltipCompat.tint(0xFFB02E26));
			if (themed != red) {
				throw new AssertionError("Shulkerfarbe im Tooltip nicht erkannt: " + Integer.toHexString(themed) + " statt " + Integer.toHexString(red));
			}
			// Shulker Box Tooltip (falls installiert) zeigt die Vorschau erst mit Shift
			context.getInput().holdShift();
			context.waitTicks(3);
			context.takeScreenshot("luhub-06d-shulker-tooltip-shift");
			context.getInput().releaseShift();
			hoverHotbarSlot(context, 1);
			context.takeScreenshot("luhub-06c-food-tooltip");
			hoverHotbarSlot(context, 0);
			context.takeScreenshot("luhub-06g-tooltip-infos");
			checkFuelTooltip(context);
			dragInventoryWindow(context);
			context.takeScreenshot("luhub-06e-window-moved");
			checkRecipeButtonFollows(context);
			toggleRecipeBook(context);
			context.takeScreenshot("luhub-06f-recipe-book-moved");
			checkRecipeButtonFollows(context);
			toggleRecipeBook(context);
			context.runOnClient(mc -> LuConfig.get().windowOffsets.clear());

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-07-hud-editor");

			context.setScreen(() -> new DebugEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-08-debug-editor");

			context.setScreen(() -> new LuConfigScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-09-config");

			context.setScreen(() -> new TooltipEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-09b-tooltip-editor");

			context.setScreen(() -> new ColorPickerScreen(null, Component.literal("Herzen"), 0xFFE86AA8, c -> { }));
			context.waitTicks(5);
			context.takeScreenshot("luhub-10-color-picker");
			context.setScreen(() -> null);

			world.getServer().runCommand("setblock 2 -60 0 minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:12},"
				+ "{Slot:4b,id:\"minecraft:amethyst_shard\",count:30},{Slot:13b,id:\"minecraft:axolotl_bucket\",count:1},"
				+ "{Slot:20b,id:\"minecraft:diamond\",count:7},{Slot:8b,id:\"minecraft:stone\",count:40},{Slot:16b,id:\"minecraft:stone\",count:30},"
				+ "{Slot:25b,id:\"minecraft:amethyst_shard\",count:5},{Slot:11b,id:\"minecraft:apple\",count:3}]}");
			world.getServer().runCommand("setblock -2 -60 0 minecraft:furnace{Items:[{Slot:0b,id:\"minecraft:raw_iron\",count:8},"
				+ "{Slot:1b,id:\"minecraft:coal\",count:4}]}");
			context.waitTicks(10);
			openContainer(context, new BlockPos(2, -60, 0), ContainerScreen.class);
			context.takeScreenshot("luhub-12-chest");
			context.runOnClient(mc -> Sorter.sort(Sorter.Region.CONTAINER));
			context.waitTicks(5);
			context.takeScreenshot("luhub-12b-chest-sorted");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(-2, -60, 0), FurnaceScreen.class);
			context.waitTicks(40);
			context.takeScreenshot("luhub-13-furnace");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);

			world.getServer().runCommand("setblock 0 -60 2 minecraft:red_shulker_box{Items:[{Slot:0b,id:\"minecraft:cake\",count:1},"
				+ "{Slot:10b,id:\"minecraft:axolotl_bucket\",count:1}]}");
			world.getServer().runCommand("setblock 0 -60 -2 minecraft:lime_shulker_box{Items:[{Slot:4b,id:\"minecraft:emerald\",count:20}]}");
			world.getServer().runCommand("setblock 2 -60 2 minecraft:crafting_table");
			context.waitTicks(10);
			openContainer(context, new BlockPos(0, -60, 2), ShulkerBoxScreen.class);
			context.takeScreenshot("luhub-14-shulker-red");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(0, -60, -2), ShulkerBoxScreen.class);
			context.takeScreenshot("luhub-15-shulker-lime");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(2, -60, 2), CraftingScreen.class);
			context.takeScreenshot("luhub-16-crafting-axo");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);

			// Alle Arbeitsblöcke verschoben: das ganze Fenster (Hintergrund!) muss mitwandern, nicht nur Items und Text
			context.runOnClient(mc -> {
				for (Class<?> type : List.of(ContainerScreen.class, ShulkerBoxScreen.class, CraftingScreen.class, FurnaceScreen.class,
					BrewingStandScreen.class, AnvilScreen.class, EnchantmentScreen.class)) {
					LuConfig.get().windowOffsets.put(type.getName(), new int[]{-70, 30});
				}
			});
			openContainer(context, new BlockPos(0, -60, 2), ShulkerBoxScreen.class);
			context.takeScreenshot("luhub-19-moved-shulker");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(2, -60, 2), CraftingScreen.class);
			context.takeScreenshot("luhub-19b-moved-crafting");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(-2, -60, 0), FurnaceScreen.class);
			context.takeScreenshot("luhub-19c-moved-furnace");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			world.getServer().runCommand("setblock 2 -60 0 minecraft:chest[facing=west,type=right]{Items:[{Slot:0b,id:\"minecraft:diamond\",count:12}]}");
			world.getServer().runCommand("setblock 2 -60 1 minecraft:chest[facing=west,type=left]{Items:[{Slot:5b,id:\"minecraft:emerald\",count:9}]}");
			world.getServer().runCommand("setblock 2 -60 2 minecraft:ender_chest[facing=west]");
			world.getServer().runCommand("setblock -2 -60 0 minecraft:brewing_stand");
			world.getServer().runCommand("setblock 0 -60 -2 minecraft:anvil");
			world.getServer().runCommand("setblock 0 -60 2 minecraft:enchanting_table");
			context.waitTicks(10);
			openContainer(context, new BlockPos(2, -60, 0), ContainerScreen.class);
			context.takeScreenshot("luhub-19d-moved-large-chest");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(2, -60, 2), ContainerScreen.class);
			context.takeScreenshot("luhub-19e-moved-ender-chest");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(-2, -60, 0), BrewingStandScreen.class);
			context.takeScreenshot("luhub-19f-moved-brewing");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(0, -60, -2), AnvilScreen.class);
			context.takeScreenshot("luhub-19g-moved-anvil");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			openContainer(context, new BlockPos(0, -60, 2), EnchantmentScreen.class);
			context.takeScreenshot("luhub-19h-moved-enchanting");
			context.getInput().pressKey(InputConstants.KEY_ESCAPE);
			context.waitTicks(5);
			context.runOnClient(mc -> LuConfig.get().windowOffsets.clear());

			if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tutorialtracker")) {
				// Tutorial Tracker (Luna-OS): Fenster im Theme?
				context.getInput().pressKey(InputConstants.KEY_B);
				context.waitTicks(10);
				context.takeScreenshot("luhub-16b-tutorial-tracker");
				context.setScreen(() -> null);
				context.waitTicks(3);
			}

			if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("plasmovoice")) {
				// Plasmo Voice: Einstellungen (Taste V) im Theme?
				context.waitTicks(40);
				context.takeScreenshot("luhub-16c-plasmo-voice-hud");
				context.getInput().setCursorPos(5, 5);
				context.getInput().pressKey(InputConstants.KEY_V);
				context.waitTicks(20);
				context.takeScreenshot("luhub-16d-plasmo-voice");
				context.setScreen(() -> null);
				context.waitTicks(3);
			}

			context.setScreen(() -> new PauseScreen(true));
			context.waitTicks(5);
			context.takeScreenshot("luhub-17-pause");
			context.setScreen(() -> new AdvancementsScreen(context.computeOnClient(mc -> mc.player.connection.getAdvancements())));
			context.waitTicks(5);
			context.takeScreenshot("luhub-18-advancements");
			context.setScreen(() -> null);

			world.getServer().runCommand("setblock ~ ~ ~ minecraft:water");
			world.getServer().runCommand("setblock ~ ~1 ~ minecraft:water");
			context.waitTicks(120);
			context.takeScreenshot("luhub-11-air");
		}
	}

	/** Zieht das Inventar an der Titelleiste nach links oben und prüft, dass die Position gespeichert wurde. */
	private static void dragInventoryWindow(ClientGameTestContext context) {
		double[] start = context.computeOnClient(mc -> {
			AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) mc.gui.screen();
			double scale = mc.getWindow().getGuiScale();
			return new double[]{(acc.luhub$getLeftPos() + 60) * scale, (acc.luhub$getTopPos() + 3) * scale, scale};
		});
		context.getInput().setCursorPos(start[0], start[1]);
		context.waitTicks(2);
		context.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);
		for (int i = 0; i < 6; i++) {
			context.getInput().moveCursor(-10 * start[2], -4 * start[2]);
			context.waitTicks(1);
		}
		context.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(5);
		int[] offset = context.computeOnClient(mc -> LuConfig.get().windowOffsets.get(mc.gui.screen().getClass().getName()));
		if (offset == null || offset[0] >= 0) {
			throw new AssertionError("Fenster wurde nicht verschoben: " + (offset == null ? "null" : offset[0] + "," + offset[1]));
		}
	}

	/** Kohle muss im Tooltip ihre Brenndauer zeigen. */
	private static void checkFuelTooltip(ClientGameTestContext context) {
		boolean fuel = context.computeOnClient(mc -> new ItemStack(Items.COAL)
			.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL).stream()
			.anyMatch(line -> line.getContents() instanceof TranslatableContents t && t.getKey().equals("luhub.tooltip.fuel")));
		if (!fuel) {
			throw new AssertionError("Brennstoff-Info fehlt im Tooltip von Kohle");
		}
	}

	/** Axo (der Rezeptbuch-Knopf) muss nach dem Verschieben noch am selben Platz im Fenster sitzen. */
	private static void checkRecipeButtonFollows(ClientGameTestContext context) {
		String problem = context.computeOnClient(mc -> {
			AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) mc.gui.screen();
			AbstractWidget button = recipeButton(mc.gui.screen());
			if (button == null) {
				return null; // REI hat das Rezeptbuch ausgeblendet – dann zeichnet Lu Hub Axo selbst am Fenster
			}
			int expected = acc.luhub$getTopPos() + acc.luhub$getImageHeight() / 2 - 22;
			return button.getY() == expected ? null : "Axo bei y=" + button.getY() + " statt " + expected;
		});
		if (problem != null) {
			throw new AssertionError(problem);
		}
	}

	private static void toggleRecipeBook(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			AbstractWidget button = recipeButton(mc.gui.screen());
			if (button instanceof Button b) {
				((ButtonAccessor) b).luhub$getOnPress().onPress(b);
			}
		});
		context.waitTicks(5);
	}

	private static AbstractWidget recipeButton(Screen screen) {
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget instanceof ImageButtonAccessor button && button.luhub$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
				return widget;
			}
		}
		return null;
	}

	/** Bewegt die Maus über einen Hotbar-Slot im Inventar, damit der Tooltip erscheint. */
	private static void hoverHotbarSlot(ClientGameTestContext context, int slot) {
		double[] pos = context.computeOnClient(mc -> {
			AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) mc.gui.screen();
			double scale = mc.getWindow().getGuiScale();
			return new double[]{(acc.luhub$getLeftPos() + 8 + slot * 18 + 8) * scale, (acc.luhub$getTopPos() + 142 + 8) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(5);
	}

	private static void openContainer(ClientGameTestContext context, BlockPos pos, Class<? extends Screen> screen) {
		context.getInput().lookAt(pos);
		context.waitTicks(2);
		context.getInput().pressKey(options -> options.keyUse);
		context.waitForScreen(screen);
		context.waitTicks(5);
	}

	private static void setStyle(ClientGameTestContext context, BarStyle style) {
		context.runOnClient(mc -> {
			for (HudElementType type : HudElementType.values()) {
				if (type.styleable) {
					LuConfig.get().hud(type).style = style;
				}
			}
		});
		context.waitTicks(3);
	}
}
