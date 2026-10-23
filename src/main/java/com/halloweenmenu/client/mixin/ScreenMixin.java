package com.halloweenmenu.client.mixin;

import com.halloweenmenu.client.ThemedBackground;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    /** Заменяет серый/размытый фон на хеллоуинскую сцену. */
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void halloween$background(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ThemedBackground.applies((Screen) (Object) this)) {
            ThemedBackground.render(context, mouseX, mouseY);
            ci.cancel();
        }
    }
}
