package dev.lunaos.luhub.sort;

import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sortieren, Einräumen und Auffüllen – rein über ganz normale Klicks, die auch ein Spieler machen könnte.
 * Dadurch funktioniert es auf jedem Server, ohne dass dort etwas installiert sein muss.
 */
public final class Sorter {
	private static final Comparator<ItemStack> ORDER = Comparator
		.<ItemStack>comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()))
		.thenComparing(stack -> stack.getHoverName().getString())
		.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed())
		.thenComparingInt(ItemStack::getDamageValue);

	private Sorter() {
	}

	public enum Region {
		/** Die Slots der Kiste / Shulkerkiste / des Trichters. */
		CONTAINER,
		/** Das Spieler-Inventar (ohne Rüstung, Zweithand und – je nach Einstellung – Hotbar). */
		INVENTORY
	}

	/** Alle Slots einer Region im aktuell geöffneten Menü. */
	public static List<Slot> slots(AbstractContainerMenu menu, Player player, Region region) {
		List<Slot> result = new ArrayList<>();
		boolean hotbar = LuConfig.get().sortHotbar;
		for (Slot slot : menu.slots) {
			boolean playerInv = slot.container instanceof Inventory inv && inv.player == player;
			if (region == Region.CONTAINER && !playerInv && slot.container.getContainerSize() >= 5) {
				result.add(slot);
			} else if (region == Region.INVENTORY && playerInv) {
				int index = slot.getContainerSlot();
				if ((index >= 9 && index < 36) || (hotbar && index < 9)) {
					result.add(slot);
				}
			}
		}
		return result;
	}

	public static Region regionOf(Slot slot, Player player) {
		return slot.container instanceof Inventory inv && inv.player == player ? Region.INVENTORY : Region.CONTAINER;
	}

	private static void click(AbstractContainerMenu menu, Slot slot, ContainerInput input) {
		Minecraft mc = Minecraft.getInstance();
		mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, input, mc.player);
	}

	/** Gleiche Stapel zusammenlegen und dann nach Item-Art sortieren. */
	public static void sort(Region region) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.gameMode == null) {
			return;
		}
		AbstractContainerMenu menu = player.containerMenu;
		if (!menu.getCarried().isEmpty()) {
			return;
		}
		List<Slot> slots = slots(menu, player, region).stream().filter(s -> s.mayPickup(player)).toList();
		if (slots.isEmpty()) {
			return;
		}
		merge(menu, slots, player);

		List<ItemStack> target = new ArrayList<>();
		for (Slot slot : slots) {
			if (slot.hasItem()) {
				target.add(slot.getItem().copy());
			}
		}
		target.sort(ORDER);

		for (int k = 0; k < slots.size(); k++) {
			Slot destination = slots.get(k);
			if (k >= target.size()) {
				break;
			}
			ItemStack wanted = target.get(k);
			if (same(destination.getItem(), wanted)) {
				continue;
			}
			Slot source = null;
			for (int j = k + 1; j < slots.size(); j++) {
				if (same(slots.get(j).getItem(), wanted)) {
					source = slots.get(j);
					break;
				}
			}
			if (source == null) {
				continue;
			}
			// Stapel aufnehmen, am Ziel ablegen (tauscht mit dem, was dort liegt) und den Rest zurücklegen
			click(menu, source, ContainerInput.PICKUP);
			click(menu, destination, ContainerInput.PICKUP);
			if (!menu.getCarried().isEmpty()) {
				click(menu, source, ContainerInput.PICKUP);
			}
			if (!menu.getCarried().isEmpty()) {
				return; // sollte nie passieren – lieber abbrechen als Items zu verlieren
			}
		}
	}

	private static void merge(AbstractContainerMenu menu, List<Slot> slots, Player player) {
		for (int i = 0; i < slots.size(); i++) {
			Slot into = slots.get(i);
			for (int j = i + 1; j < slots.size(); j++) {
				ItemStack target = into.getItem();
				if (target.isEmpty() || target.getCount() >= target.getMaxStackSize()) {
					break;
				}
				Slot from = slots.get(j);
				if (from.hasItem() && ItemStack.isSameItemSameComponents(target, from.getItem())) {
					click(menu, from, ContainerInput.PICKUP);
					click(menu, into, ContainerInput.PICKUP);
					if (!menu.getCarried().isEmpty()) {
						click(menu, from, ContainerInput.PICKUP);
					}
					if (!menu.getCarried().isEmpty()) {
						return;
					}
				}
			}
		}
	}

	private static boolean same(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
	}

	/** Einräumen: alles aus dem Inventar in die Kiste schieben, was dort schon liegt. */
	public static void quickStack() {
		transfer(Region.INVENTORY, Region.CONTAINER);
	}

	/** Auffüllen: aus der Kiste alles ins Inventar holen, wovon man schon etwas dabei hat. */
	public static void restock() {
		transfer(Region.CONTAINER, Region.INVENTORY);
	}

	private static void transfer(Region from, Region to) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.gameMode == null || !player.containerMenu.getCarried().isEmpty()) {
			return;
		}
		AbstractContainerMenu menu = player.containerMenu;
		List<Slot> targetSlots = slots(menu, player, to);
		if (to == Region.INVENTORY) {
			// beim Auffüllen zählt auch, was in der Hotbar liegt
			targetSlots = new ArrayList<>(targetSlots);
			for (Slot slot : menu.slots) {
				if (slot.container instanceof Inventory inv && inv.player == player && slot.getContainerSlot() < 9 && !targetSlots.contains(slot)) {
					targetSlots.add(slot);
				}
			}
		}
		for (Slot slot : slots(menu, player, from)) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty() || !slot.mayPickup(player)) {
				continue;
			}
			boolean present = targetSlots.stream().anyMatch(t -> ItemStack.isSameItemSameComponents(t.getItem(), stack));
			if (present) {
				click(menu, slot, ContainerInput.QUICK_MOVE);
			}
		}
	}
}
