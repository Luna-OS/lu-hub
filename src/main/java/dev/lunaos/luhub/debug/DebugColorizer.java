package dev.lunaos.luhub.debug;

import dev.lunaos.luhub.config.LuConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markiert F3-Zeilen farbig: Bezeichnung (vor dem Doppelpunkt) in der Panel-Farbe, Zahlen hervorgehoben,
 * Überschriften in eigener Farbe und die FPS je nach Wert grün/gelb/rot.
 */
public final class DebugColorizer {
	private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:[.,]\\d+)?%?");
	private static final Pattern FPS = Pattern.compile("^(\\d+) fps");

	private DebugColorizer() {
	}

	public static Component colorize(String raw, int accent) {
		LuConfig c = LuConfig.get();
		boolean header = raw.contains("§n") || raw.contains("§l");
		String line = LuDebugOverlay.stripFormatting(raw);
		MutableComponent out = Component.empty();
		if (header) {
			return out.append(Component.literal(line).withColor(rgb(c.debugHeaderColor)).withStyle(s -> s.withUnderlined(true)));
		}

		Matcher fps = FPS.matcher(line);
		if (fps.find() && c.debugFpsColors) {
			int value = Integer.parseInt(fps.group(1));
			int color = value >= 60 ? 0x7CFF9A : value >= 30 ? 0xFFE066 : 0xFF6B6B;
			out.append(Component.literal(fps.group(1)).withColor(color));
			out.append(Component.literal(" fps").withColor(rgb(accent)));
			appendValue(out, line.substring(fps.end()), c);
			return out;
		}

		int colon = line.indexOf(':');
		if (colon > 0 && colon < 40 && (colon == line.length() - 1 || line.charAt(colon + 1) == ' ')) {
			out.append(Component.literal(line.substring(0, colon + 1)).withColor(rgb(accent)));
			appendValue(out, line.substring(colon + 1), c);
		} else {
			appendValue(out, line, c);
		}
		return out;
	}

	private static void appendValue(MutableComponent out, String value, LuConfig c) {
		Matcher m = NUMBER.matcher(value);
		int last = 0;
		while (m.find()) {
			// Zahlen, die Teil eines Wortes sind (z.B. "minecraft:block_1"), nicht einfärben
			if (m.start() > 0 && Character.isLetter(value.charAt(m.start() - 1))) {
				continue;
			}
			if (m.start() > last) {
				out.append(Component.literal(value.substring(last, m.start())).withColor(rgb(c.debugValueColor)));
			}
			out.append(Component.literal(m.group()).withColor(rgb(c.debugNumberColor)));
			last = m.end();
		}
		if (last < value.length()) {
			out.append(Component.literal(value.substring(last)).withColor(rgb(c.debugValueColor)));
		}
	}

	private static int rgb(int argb) {
		return argb & 0xFFFFFF;
	}
}
