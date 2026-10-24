package dev.lunaos.luhub.tooltip;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Alle Zusatz-Infos, die Lu Hub in Item-Tooltips anzeigen kann. */
public enum TooltipInfo {
	DURABILITY(true, 0),
	INVENTORY_COUNT(true, 0xFFB9C0FF),
	REPAIR_ITEM(true, 0xFF9AA4D8),
	REPAIR_COST(true, 0xFFC9A0FF),
	FUEL(true, 0xFFFFB86B),
	BREWING_FUEL(true, 0xFFFFB86B),
	COMPOST(true, 0xFF8FD18F),
	FOOD_VALUES(false, 0xFFFFD27F),
	LIGHT(true, 0xFFFFE08A),
	STACK_SIZE(false, 0xFF9AA4D8),
	ENCHANTABILITY(false, 0xFFC9A0FF),
	BLAST_RESISTANCE(false, 0xFF9AA4D8),
	MOD_NAME(true, 0xFF7F8CFF),
	ITEM_ID(false, 0xFF6B6F8F);

	public final boolean defaultEnabled;
	public final int defaultColor;

	TooltipInfo(boolean defaultEnabled, int defaultColor) {
		this.defaultEnabled = defaultEnabled;
		this.defaultColor = defaultColor;
	}

	public Component label() {
		return Component.translatable("luhub.tooltip_info." + name().toLowerCase(Locale.ROOT));
	}

	public static TooltipInfo byId(String id) {
		for (TooltipInfo info : values()) {
			if (info.name().equals(id)) {
				return info;
			}
		}
		return null;
	}
}
