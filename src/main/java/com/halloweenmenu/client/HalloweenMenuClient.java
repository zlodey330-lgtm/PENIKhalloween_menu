package com.halloweenmenu.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HalloweenMenuClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(HalloweenMusic::tick);

        // В ванильном главном меню добавляем кнопку возврата в хэллоуинское меню
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && HalloweenState.vanilla) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("Хэллоуинское меню"), button -> {
                            HalloweenState.vanilla = false;
                            client.setScreen(new HalloweenScreen());
                        }).dimensions(6, 6, 130, 20).build());
            }
        });
    }
}
