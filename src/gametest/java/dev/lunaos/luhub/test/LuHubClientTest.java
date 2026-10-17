package dev.lunaos.luhub.test;

import dev.lunaos.luhub.config.BarStyle;
import dev.lunaos.luhub.config.HudElementType;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.screen.ColorPickerScreen;
import dev.lunaos.luhub.screen.DebugEditorScreen;
import dev.lunaos.luhub.screen.HudEditorScreen;
import dev.lunaos.luhub.screen.LuConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Startet eine Testwelt und macht Screenshots aller Lu-Hub-Features. */
public final class LuHubClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set midnight");
			world.getServer().runCommand("give @a minecraft:diamond_sword");
			world.getServer().runCommand("give @a minecraft:golden_apple 5");
			world.getServer().runCommand("give @a minecraft:torch 32");
			world.getServer().runCommand("xp add @a 7 levels");
			world.getServer().runCommand("xp add @a 40 points");
			world.getServer().runCommand("damage @a 7");
			world.getServer().runCommand("effect give @a absorption 600 0 true");
			world.getServer().runCommand("effect give @a hunger 3 200 true");
			context.waitTicks(80);
			context.takeScreenshot("luhub-01-hud-night");

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

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-07-hud-editor");

			context.setScreen(() -> new DebugEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-08-debug-editor");

			context.setScreen(() -> new LuConfigScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("luhub-09-config");

			context.setScreen(() -> new ColorPickerScreen(null, Component.literal("Herzen"), 0xFFE86AA8, c -> { }));
			context.waitTicks(5);
			context.takeScreenshot("luhub-10-color-picker");
			context.setScreen(() -> null);

			world.getServer().runCommand("setblock 2 -60 0 minecraft:chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:12},"
				+ "{Slot:4b,id:\"minecraft:amethyst_shard\",count:30},{Slot:13b,id:\"minecraft:axolotl_bucket\",count:1}]}");
			world.getServer().runCommand("setblock -2 -60 0 minecraft:furnace{Items:[{Slot:0b,id:\"minecraft:raw_iron\",count:8},"
				+ "{Slot:1b,id:\"minecraft:coal\",count:4}]}");
			context.waitTicks(10);
			openContainer(context, new BlockPos(2, -60, 0), ContainerScreen.class);
			context.takeScreenshot("luhub-12-chest");
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
