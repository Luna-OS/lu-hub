package dev.lunaos.luhub.compat.rei;

import com.mojang.serialization.MapCodec;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import net.minecraft.network.codec.StreamCodec;

/** Lu-Hub-Displays entstehen nur auf dem Client und werden nie gespeichert oder übertragen. */
final class ClientOnlySerializer {
	private ClientOnlySerializer() {
	}

	static <D extends Display> DisplaySerializer<D> create() {
		return DisplaySerializer.of(
			MapCodec.unit(() -> {
				throw new UnsupportedOperationException("Lu-Hub-Displays werden nicht gespeichert");
			}),
			StreamCodec.of((buf, display) -> {
				throw new UnsupportedOperationException("Lu-Hub-Displays werden nicht übertragen");
			}, buf -> {
				throw new UnsupportedOperationException("Lu-Hub-Displays werden nicht übertragen");
			}),
			false);
	}
}
