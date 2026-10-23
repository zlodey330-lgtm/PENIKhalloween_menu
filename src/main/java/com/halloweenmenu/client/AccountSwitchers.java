package com.halloweenmenu.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;

/**
 * Находит установленный мод для смены аккаунта и умеет открывать его экран.
 * Если такого мода нет, кнопка в меню не показывается.
 */
final class AccountSwitchers {

    /** {id мода, класс экрана с конструктором (Screen parent)}. */
    private static final String[][] CANDIDATES = {
            {"ias", "ru.vidtu.ias.screen.AccountScreen"},
            {"authme", "me.axieum.mcmod.authme.impl.gui.screen.AuthScreen"},
            {"authme", "me.axieum.mcmod.authme.api.gui.screen.AuthScreen"},
            {"accountswitcher", "the_fireplace.ias.gui.GuiAccountSelector"},
    };

    private static boolean checked;
    private static Constructor<?> ctor;

    private AccountSwitchers() {
    }

    private static void check() {
        if (checked) {
            return;
        }
        checked = true;
        for (String[] c : CANDIDATES) {
            if (!FabricLoader.getInstance().isModLoaded(c[0])) {
                continue;
            }
            try {
                Class<?> cls = Class.forName(c[1]);
                ctor = cls.getConstructor(Screen.class);
                return;
            } catch (Throwable ignored) {
                // пробуем следующий вариант
            }
        }
    }

    static boolean available() {
        check();
        return ctor != null;
    }

    static Screen create(Screen parent) {
        check();
        if (ctor == null) {
            return null;
        }
        try {
            return (Screen) ctor.newInstance(parent);
        } catch (Throwable t) {
            return null;
        }
    }
}
