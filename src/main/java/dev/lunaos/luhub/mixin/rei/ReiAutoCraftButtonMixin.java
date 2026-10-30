package dev.lunaos.luhub.mixin.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.display.Display;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * REI zeigt neben jeder Seite einen Knopf zum Einfüllen der Zutaten – bei Lu-Hub-Seiten (Erz-Höhen, Dorfbewohner)
 * gibt es nichts einzufüllen, dort erschien nur ein „!“. Für diese Seiten wird der Knopf weggelassen.
 */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.widget.InternalWidgets", remap = false)
public abstract class ReiAutoCraftButtonMixin {
	@Inject(method = "createAutoCraftingButtonWidget", at = @At("HEAD"), cancellable = true, require = 0)
	private static void luhub$noButtonForLuHub(Rectangle displayBounds, Rectangle rectangle, Component text,
		Supplier<Display> displaySupplier, Supplier<Collection<Identifier>> idsSupplier, List<Widget> setupDisplay,
		DisplayCategory<?> category, CallbackInfoReturnable<Widget> cir) {
		if (category != null && "luhub".equals(category.getCategoryIdentifier().getNamespace())) {
			cir.setReturnValue(Widgets.noOp());
		}
	}
}
