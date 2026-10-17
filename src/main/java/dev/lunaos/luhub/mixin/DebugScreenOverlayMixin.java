package dev.lunaos.luhub.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.lunaos.luhub.debug.LuDebugOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Fängt die Zeilen der Vanilla-F3-Einträge ab und zeichnet sie stattdessen als eigene,
 * verschiebbare und farbige Panels. Diagramme (Profiler, FPS, Netzwerk) bleiben Vanilla.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void luhub$begin(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (LuDebugOverlay.editorOpen) {
			ci.cancel();
			return;
		}
		if (LuDebugOverlay.isActive()) {
			LuDebugOverlay.beginFrame();
		}
	}

	@WrapOperation(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntry;display(Lnet/minecraft/client/gui/components/debug/DebugScreenDisplayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/chunk/LevelChunk;)V")
	)
	private void luhub$captureEntry(DebugScreenEntry entry, DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk,
									LevelChunk serverChunk, Operation<Void> original) {
		DebugScreenDisplayer target = LuDebugOverlay.isActive() ? LuDebugOverlay.capture(entry, displayer) : displayer;
		original.call(entry, target, level, clientChunk, serverChunk);
	}

	@WrapOperation(
		method = "extractRenderState",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;ZI)V")
	)
	private void luhub$skipVanillaLines(DebugScreenOverlay overlay, GuiGraphicsExtractor graphics, List<String> lines, boolean alignLeft,
										int screenWidth, Operation<Void> original) {
		if (!LuDebugOverlay.isActive()) {
			original.call(overlay, graphics, lines, alignLeft, screenWidth);
		}
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void luhub$end(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		LuDebugOverlay.endFrame(graphics);
	}
}
