package com.halloweenmenu.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Плейлист главного меню: трек 1 -> трек 2 -> трек 1 -> ... бесконечно.
 * Громкость регулируется ползунком «Музыка» в настройках звука.
 */
public final class HalloweenMusic {

    public static final String[] NAMES = {"Трек 1", "Трек 2"};
    private static final String[] IDS = {"track1", "track2"};

    private static boolean active;
    private static boolean paused;
    private static int index;
    private static SoundInstance current;
    private static int graceTicks;

    private HalloweenMusic() {
    }

    public static int index() {
        return index;
    }

    public static String currentName() {
        return NAMES[index];
    }

    public static boolean isPaused() {
        return paused;
    }

    /** Вызывается, когда показано меню (включает плейлист). */
    public static void activate() {
        active = true;
    }

    public static void next() {
        select((index + 1) % IDS.length);
    }

    public static void previous() {
        select((index + IDS.length - 1) % IDS.length);
    }

    public static void select(int i) {
        index = ((i % IDS.length) + IDS.length) % IDS.length;
        paused = false;
        stopCurrent();
    }

    public static void togglePause() {
        paused = !paused;
        if (paused) {
            stopCurrent();
        }
    }

    /** Полностью выключает музыку (когда игрок зашёл в мир). */
    public static void shutdown() {
        active = false;
        stopCurrent();
    }

    private static void stopCurrent() {
        if (current != null) {
            MinecraftClient.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    /** Каждый тик клиента. */
    public static void tick(MinecraftClient client) {
        if (client.world != null) {
            if (active) {
                shutdown();
            }
            return;
        }
        Screen screen = client.currentScreen;
        if (screen instanceof HalloweenScreen) {
            // вышли из мира в хэллоуинское меню: включаем плейлист заново
            active = true;
        } else if (screen instanceof TitleScreen) {
            // открыто ванильное главное меню: пусть играет ванильная музыка
            if (active) {
                shutdown();
            }
            return;
        }
        if (!active) {
            return;
        }
        // убираем ванильную музыку меню
        client.getMusicTracker().stop();

        if (paused) {
            return;
        }
        if (graceTicks > 0) {
            graceTicks--;
            return;
        }
        if (current != null && client.getSoundManager().isPlaying(current)) {
            return;
        }
        if (current != null) {
            // трек закончился -> следующий
            index = (index + 1) % IDS.length;
        }
        SoundEvent event = SoundEvent.of(Identifier.of("halloween_menu", IDS[index]));
        current = PositionedSoundInstance.music(event);
        client.getSoundManager().play(current);
        graceTicks = 20;
    }
}
