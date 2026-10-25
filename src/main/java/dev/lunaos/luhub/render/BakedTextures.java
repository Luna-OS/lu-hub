package dev.lunaos.luhub.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Kleine, zur Laufzeit erzeugte Texturen (HUD-Symbole, XP-Leiste, Sternenhimmel, Mond).
 * <p>
 * Minecraft 26.x sortiert jedes GUI-Element einzeln in seine Ebenen ein – tausende 1×1-fill()-Aufrufe pro Frame
 * werden dadurch sehr teuer. Deshalb wird alles, was sich nur bei Einstellungs- oder Größenänderungen ändert,
 * einmal in eine Textur gezeichnet und danach mit einem einzigen blit() dargestellt.
 * <p>
 * Freigegeben werden Texturen erst im nächsten Client-Tick: so ist sichergestellt, dass der aktuelle Frame,
 * der sie eventuell noch verwendet, schon fertig gezeichnet ist.
 */
public final class BakedTextures {
	@FunctionalInterface
	public interface PixelSource {
		/** ARGB-Farbe des Pixels (0 = durchsichtig). */
		int color(int x, int y);
	}

	private static final List<Identifier> PENDING_RELEASE = new ArrayList<>();
	private static int counter;

	private BakedTextures() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> flush());
	}

	/** Zeichnet ein Bild der Größe width×height Pixel für Pixel und registriert es als Textur. */
	public static Identifier bake(String name, int width, int height, PixelSource source) {
		NativeImage image = new NativeImage(width, height, true);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = source.color(x, y);
				if (color != 0) {
					image.setPixel(x, y, color);
				}
			}
		}
		return upload(name, image);
	}

	/** Registriert ein fertiges Bild als Textur (das Bild gehört danach der Textur). */
	public static Identifier upload(String name, NativeImage image) {
		Identifier id = Identifier.fromNamespaceAndPath("luhub", "dynamic/" + name + "_" + counter++);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
		return id;
	}

	/** Gibt eine Textur frei – verzögert bis zum nächsten Tick. */
	public static void release(Identifier id) {
		if (id != null) {
			PENDING_RELEASE.add(id);
		}
	}

	private static void flush() {
		if (PENDING_RELEASE.isEmpty()) {
			return;
		}
		for (Identifier id : PENDING_RELEASE) {
			Minecraft.getInstance().getTextureManager().release(id);
		}
		PENDING_RELEASE.clear();
	}
}
