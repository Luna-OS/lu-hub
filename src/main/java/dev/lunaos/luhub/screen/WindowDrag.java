package dev.lunaos.luhub.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lunaos.luhub.compat.ExternalMods;
import dev.lunaos.luhub.compat.TutorialTrackerCompat;
import dev.lunaos.luhub.config.LuConfig;
import dev.lunaos.luhub.mixin.AbstractContainerScreenAccessor;
import dev.lunaos.luhub.mixin.AbstractRecipeBookScreenAccessor;
import dev.lunaos.luhub.mixin.ButtonAccessor;
import dev.lunaos.luhub.theme.ThemeDecor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Inventar, Kisten, Öfen &amp; Co. per Drag &amp; Drop verschieben: an der Titelleiste ziehen
 * (oder mit gedrückter Alt-Taste irgendwo im Fenster). Die Position wird pro Fenstertyp gespeichert,
 * ein Doppelklick auf die Titelleiste setzt sie zurück.
 */
public final class WindowDrag {
	private static AbstractContainerScreen<?> dragging;
	private static double startMouseX;
	private static double startMouseY;
	private static int startOffsetX;
	private static int startOffsetY;
	private static long lastTitleClick;
	/** Knöpfe anderer Mods, die sich an der Fenstermitte statt am Fenster ausrichten: ihre ursprüngliche Position. */
	private static final Map<AbstractWidget, int[]> CENTERED_WIDGETS = new WeakHashMap<>();

	private WindowDrag() {
	}

	private static String key(AbstractContainerScreen<?> screen) {
		return screen.getClass().getName();
	}

	private static int[] offset(AbstractContainerScreen<?> screen) {
		int[] offset = LuConfig.get().windowOffsets.get(key(screen));
		return offset != null && LuConfig.get().windowDrag ? offset : new int[]{0, 0};
	}

	/** Ursprüngliche linke obere Ecke je Fenster (vor der Verschiebung) – für REI. */
	private static final Map<AbstractContainerScreen<?>, int[]> UNMOVED = new WeakHashMap<>();

	/**
	 * Grenzen des Fensters, wie sie ohne Verschiebung wären: [x, y, Breite, Höhe] – oder null, wenn es nicht verschoben ist.
	 * REI richtet sich danach und bleibt so an seinem Platz.
	 */
	public static int[] unmovedBounds(AbstractContainerScreen<?> screen) {
		int[] base = UNMOVED.get(screen);
		int[] offset = offset(screen);
		if (base == null || (offset[0] == 0 && offset[1] == 0 && dragging != screen)) {
			return null;
		}
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		return new int[]{base[0], base[1], acc.luhub$getImageWidth(), acc.luhub$getImageHeight()};
	}

	/** Am Ende von AbstractContainerScreen.init: gespeicherte Verschiebung anwenden (im Bildschirm gehalten). */
	public static void applyOffset(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor baseAcc = (AbstractContainerScreenAccessor) screen;
		UNMOVED.put(screen, new int[]{baseAcc.luhub$getLeftPos(), baseAcc.luhub$getTopPos()});
		int[] offset = offset(screen);
		if (offset[0] == 0 && offset[1] == 0) {
			return;
		}
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		int width = acc.luhub$getImageWidth();
		int height = acc.luhub$getImageHeight();
		acc.luhub$setLeftPos(clamp((screen.width - width) / 2 + offset[0], screen.width - width));
		acc.luhub$setTopPos(clamp((screen.height - height) / 2 + offset[1], screen.height - height));
	}

	/** Fenster mit Rezeptbuch rechnen ihre X-Position selbst aus – hier kommt die Verschiebung dazu. */
	public static int offsetLeft(int leftPos, boolean bookBesideWindow) {
		if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
			return leftPos;
		}
		int[] base = UNMOVED.get(screen);
		if (base != null) {
			base[0] = leftPos; // Fenster mit Rezeptbuch: das ist die eigentliche Ausgangsposition
		}
		return leftPos + recipeBookShift(bookBesideWindow)[0];
	}

	/**
	 * Wie weit ein Fenster mit Rezeptbuch verschoben ist: [x, y]. Das Rezeptbuch wandert genauso weit mit;
	 * steht es links neben dem Fenster, bleiben auch seine Reiter im Bild.
	 */
	public static int[] recipeBookShift(boolean bookBesideWindow) {
		if (!(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen)) {
			return new int[]{0, 0};
		}
		int[] offset = offset(screen);
		if (offset[0] == 0 && offset[1] == 0) {
			return offset;
		}
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		int width = acc.luhub$getImageWidth();
		int height = acc.luhub$getImageHeight();
		// gleiche Rechnung wie RecipeBookComponent.updateScreenPosition / getXOrigin
		int baseLeft = bookBesideWindow ? 177 + (screen.width - width - 200) / 2 : (screen.width - width) / 2;
		int minLeft = bookBesideWindow ? Math.max(0, baseLeft - ((screen.width - 147) / 2 - 86 - 30)) : 0;
		int left = Math.max(minLeft, Math.min(baseLeft + offset[0], screen.width - width));
		int baseTop = (screen.height - height) / 2;
		return new int[]{left - baseLeft, clamp(baseTop + offset[1], screen.height - height) - baseTop};
	}

	/** Wie weit das Fenster gerade senkrecht verschoben ist (für Knöpfe, die an der Bildschirmmitte hängen). */
	public static int shiftY(AbstractContainerScreen<?> screen) {
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		return acc.luhub$getTopPos() - (screen.height - acc.luhub$getImageHeight()) / 2;
	}

	/** Ist das Rezeptbuch offen? Dann wird beim Ziehen alles neu angeordnet, damit es mitwandert. */
	private static boolean recipeBookOpen(AbstractContainerScreen<?> screen) {
		return screen instanceof AbstractRecipeBookScreen<?>
			&& ((AbstractRecipeBookScreenAccessor) screen).luhub$getRecipeBookComponent().isVisible();
	}

	private static int clamp(int value, int max) {
		return Math.clamp(value, 0, Math.max(0, max));
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?> container)) {
				return;
			}
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> {
				if (!LuConfig.get().windowDrag || event.button() != InputConstants.MOUSE_BUTTON_LEFT
					|| !container.getMenu().getCarried().isEmpty()) {
					return true;
				}
				boolean alt = client.hasAltDown();
				boolean titleBar = onTitleBar(container, event.x(), event.y());
				if (!alt && !titleBar) {
					return true;
				}
				long now = net.minecraft.util.Util.getMillis();
				if (titleBar && now - lastTitleClick < 350) {
					lastTitleClick = 0;
					reset(container);
					return false;
				}
				lastTitleClick = titleBar ? now : 0;
				dragging = container;
				startMouseX = event.x();
				startMouseY = event.y();
				int[] offset = offset(container);
				startOffsetX = offset[0];
				startOffsetY = offset[1];
				return false;
			});
			ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> {
				if (dragging != container) {
					return true;
				}
				AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) container;
				int newX = startOffsetX + (int) Math.round(event.x() - startMouseX);
				int newY = startOffsetY + (int) Math.round(event.y() - startMouseY);
				if (recipeBookOpen(container)) {
					// Rezeptbuch offen: alles neu anordnen, damit Buch, Reiter und Suchfeld mitwandern
					int[] old = offset(container);
					if (old[0] != newX || old[1] != newY) {
						LuConfig.get().windowOffsets.put(key(container), new int[]{newX, newY});
						container.resize(container.width, container.height);
					}
					return false;
				}
				int targetLeft = clamp((container.width - acc.luhub$getImageWidth()) / 2 + newX, container.width - acc.luhub$getImageWidth());
				int targetTop = clamp((container.height - acc.luhub$getImageHeight()) / 2 + newY, container.height - acc.luhub$getImageHeight());
				int moveX = targetLeft - acc.luhub$getLeftPos();
				int moveY = targetTop - acc.luhub$getTopPos();
				if (moveX != 0 || moveY != 0) {
					// live mitbewegen: Fenster und alle Knöpfe verschieben
					acc.luhub$setLeftPos(targetLeft);
					acc.luhub$setTopPos(targetTop);
					for (AbstractWidget widget : Screens.getWidgets(container)) {
						if (!followsCenter(widget)) {
							widget.setPosition(widget.getX() + moveX, widget.getY() + moveY);
						}
					}
				}
				LuConfig.get().windowOffsets.put(key(container), new int[]{
					targetLeft - (container.width - acc.luhub$getImageWidth()) / 2,
					targetTop - (container.height - acc.luhub$getImageHeight()) / 2
				});
				return false;
			});
			ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> {
				if (dragging != container) {
					return true;
				}
				dragging = null;
				int[] offset = LuConfig.get().windowOffsets.get(key(container));
				if (offset != null && offset[0] == 0 && offset[1] == 0) {
					LuConfig.get().windowOffsets.remove(key(container));
				}
				LuConfig.save();
				// alles sauber neu anordnen (Rezeptbuch, REI, Sortier-Knöpfe ...)
				container.resize(container.width, container.height);
				return false;
			});
			ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> moveCenteredWidgets(container));
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				LuConfig c = LuConfig.get();
				if (!c.windowDrag) {
					return;
				}
				AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) container;
				boolean active = dragging == container;
				if (active || (container.getMenu().getCarried().isEmpty() && onTitleBar(container, mouseX, mouseY))) {
					// dezenter Rahmen zeigt: hier kann man ziehen
					int color = active ? c.colorHighlight : (c.colorHighlight & 0xFFFFFF) | 0x80000000;
					graphics.outline(acc.luhub$getLeftPos() - 1, acc.luhub$getTopPos() - 1,
						acc.luhub$getImageWidth() + 2, acc.luhub$getImageHeight() + 2, color);
				}
			});
		});
	}

	/** Knöpfe von Mods, die ihre Position aus der Bildschirmmitte berechnen (z.B. Tutorial Tracker). */
	private static boolean followsCenter(AbstractWidget widget) {
		return ExternalMods.TUTORIAL_TRACKER && isTutorialTrackerWidget(widget);
	}

	/**
	 * Solche Knöpfe würden beim Verschieben an der alten Stelle bleiben: hier wandern sie mit dem Fenster mit.
	 * Tutorial Tracker baut seinen Knopf mit Button.builder – erkannt wird er am Tooltip-/Klick-Lambda aus dessen Paket.
	 */
	private static void moveCenteredWidgets(AbstractContainerScreen<?> screen) {
		if (!ExternalMods.TUTORIAL_TRACKER) {
			return;
		}
		int[] offset = offset(screen);
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (!isTutorialTrackerWidget(widget)) {
				continue;
			}
			int[] base = CENTERED_WIDGETS.computeIfAbsent(widget, w -> new int[]{w.getX(), w.getY()});
			widget.setPosition(base[0] + offset[0], base[1] + offset[1]);
		}
	}

	private static boolean isTutorialTrackerWidget(AbstractWidget widget) {
		if (widget.getClass().getName().startsWith(TutorialTrackerCompat.PACKAGE)) {
			return true;
		}
		// Button.builder erzeugt eine Vanilla-Klasse – die Klick-Aktion verrät, von welcher Mod der Knopf stammt
		if (widget instanceof ButtonAccessor button) {
			return button.luhub$getOnPress().getClass().getName().startsWith(TutorialTrackerCompat.PACKAGE);
		}
		return false;
	}

	/** Doppelklick auf die Titelleiste: Position dieses Fenstertyps zurücksetzen. */
	public static void reset(AbstractContainerScreen<?> screen) {
		LuConfig.get().windowOffsets.remove(key(screen));
		LuConfig.save();
		screen.resize(screen.width, screen.height);
	}

	/** Liegt der Punkt auf der Titelleiste (oberer Rand bis zur ersten Slot-Reihe, ohne Knöpfe und Mond)? */
	public static boolean onTitleBar(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
		AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) screen;
		int left = acc.luhub$getLeftPos();
		int top = acc.luhub$getTopPos();
		if (mouseX < left || mouseX >= left + acc.luhub$getImageWidth() || mouseY < top) {
			return false;
		}
		int firstSlot = Integer.MAX_VALUE;
		for (Slot slot : screen.getMenu().slots) {
			if (slot.isActive()) {
				firstSlot = Math.min(firstSlot, slot.y);
			}
		}
		int barHeight = Math.min(acc.luhub$getTitleLabelY() + 10, firstSlot == Integer.MAX_VALUE ? 16 : firstSlot);
		if (mouseY >= top + barHeight) {
			return false;
		}
		for (AbstractWidget widget : Screens.getWidgets(screen)) {
			if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
				return false;
			}
		}
		LuConfig c = LuConfig.get();
		return !(c.themeEnabled && c.moonDecoration && ThemeDecor.isOverMoon(screen, mouseX, mouseY));
	}
}
