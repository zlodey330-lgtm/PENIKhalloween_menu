package com.halloweenmenu.client.mixin;

import com.halloweenmenu.client.HalloweenScreen;
import com.halloweenmenu.client.HalloweenState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    public ClientWorld world;

    /** Подменяет ванильное главное меню на хеллоуинское. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen halloween$replaceTitle(Screen screen) {
        if (screen == null) {
            if (this.world != null) {
                // игрок зашёл в мир
                HalloweenState.vanilla = false;
                return null;
            }
            // экрана нет, а мира тоже нет: игра сама открыла бы ванильное меню
            return HalloweenState.vanilla ? null : new HalloweenScreen();
        }
        if (screen.getClass() == TitleScreen.class && !HalloweenState.vanilla) {
            return new HalloweenScreen();
        }
        return screen;
    }
}
