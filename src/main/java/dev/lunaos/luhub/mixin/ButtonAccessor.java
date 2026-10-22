package dev.lunaos.luhub.mixin;

import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Button.class)
public interface ButtonAccessor {
	@Accessor("onPress")
	Button.OnPress luhub$getOnPress();
}
