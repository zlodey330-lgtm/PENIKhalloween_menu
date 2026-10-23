package com.halloweenmenu.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;

/** Хеллоуинский фон и стиль для всех экранов вне мира: выбор мира, сеть, настройки и их вкладки. */
public final class ThemedBackground {

    private static final long START = System.currentTimeMillis();
    private static ParallaxScene scene;

    private ThemedBackground() {
    }

    /** Нужно ли оформлять этот экран (только вне мира; внутри игры всё ванильное). */
    public static boolean applies(Screen screen) {
        if (screen == null) {
            return false;
        }
        if (MinecraftClient.getInstance().world != null) {
            return false;
        }
        // главное меню рисует себя само; ванильное главное меню оставляем ванильным
        return !(screen instanceof TitleScreen) && !(screen instanceof HalloweenScreen);
    }

    public static void render(DrawContext ctx, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (scene == null) {
            scene = new ParallaxScene();
            try {
                client.getTextureManager().bindTexture(McPainter.SCENE_TEXTURE);
                client.getTextureManager().getTexture(McPainter.SCENE_TEXTURE).setFilter(true, false);
            } catch (Throwable ignored) {
                // не критично
            }
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        double t = (System.currentTimeMillis() - START) / 1000.0 + 1.0;

        McPainter.begin(ctx);
        McPainter painter = new McPainter(ctx);
        scene.render(painter, w, h, t, mouseX / (float) Math.max(1, w), mouseY / (float) Math.max(1, h));
        painter.flush();
        McPainter.end();

        // затемнение, чтобы список читался, и тёплое оранжевое свечение снизу
        ctx.fill(0, 0, w, h, 0xA00A0612);
        ctx.fillGradient(0, h - h / 3, w, h, 0x00FF8A1C, 0x40FF6A00);
    }
}
