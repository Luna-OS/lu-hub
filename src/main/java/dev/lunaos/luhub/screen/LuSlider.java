package dev.lunaos.luhub.screen;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Einfacher Schieberegler für Kommazahlen (z.B. Größe, Sternendichte). */
public final class LuSlider extends AbstractSliderButton {
	private final Component label;
	private final float min;
	private final float max;
	private final float step;
	private final Consumer<Float> setter;

	public LuSlider(int x, int y, int width, Component label, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
		super(x, y, width, 20, Component.empty(), (getter.get() - min) / (max - min));
		this.label = label;
		this.min = min;
		this.max = max;
		this.step = step;
		this.setter = setter;
		updateMessage();
	}

	private float current() {
		float raw = (float) (min + value * (max - min));
		return Math.round(raw / step) * step;
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.empty().append(label).append(": ").append(String.format(Locale.ROOT, "%.2f", current())));
	}

	@Override
	protected void applyValue() {
		setter.accept(current());
	}
}
