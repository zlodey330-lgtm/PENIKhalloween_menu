package com.halloweenmenu.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Хеллоуинское главное меню с живой 3D-сценой, кнопками и плеером. */
public final class HalloweenScreen extends Screen {

    private static ParallaxScene scene;

    private static final String[] PUMPKIN = {
            "....ss.....",
            "...ss......",
            "..ooooooo..",
            ".ooooooooo.",
            "ookkooookko",
            "ookkooookko",
            "ooooookoooo",
            "okkkkkkkkko",
            ".okkokkokko",
            "..ooooooo.."
    };

    private static final String[] BAT = {
            "k.......k",
            "kk.kkk.kk",
            "kkkkkkkkk",
            "kkkkkkkkk",
            ".kk.k.kk.",
            "..k...k.."
    };

    private static final String[] GHOST = {
            ".kkkkk.",
            "kkkkkkk",
            "kk.k.kk",
            "kkkkkkk",
            "kkkkkkk",
            "kkkkkkk",
            "k.kkk.k"
    };

    private static final String[] SKULL = {
            ".kkkkk.",
            "kkkkkkk",
            "k..k..k",
            "kkkkkkk",
            ".kkkkk.",
            ".k.k.k."
    };

    private static final class Rect {
        int x, y, w, h;
        String label;
        Runnable action;
        boolean big;
        String[] icon;

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final long start = System.currentTimeMillis();
    private final List<Rect> rects = new ArrayList<>();
    private Rect prevBtn, playBtn, nextBtn;
    private String notice = "";
    private long noticeUntil;
    private int panelX, panelW, titleY, accountY;
    private float titleScale = 3.0f;

    public HalloweenScreen() {
        super(Text.literal("Halloween"));
    }

    @Override
    protected void init() {
        HalloweenMusic.activate();
        if (scene == null) {
            scene = new ParallaxScene();
        }
        try {
            // сглаживание текстуры сцены (иначе будет «пикселями»)
            client.getTextureManager().bindTexture(McPainter.SCENE_TEXTURE);
            client.getTextureManager().getTexture(McPainter.SCENE_TEXTURE).setFilter(true, false);
        } catch (Throwable ignored) {
            // не критично
        }
        layout();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private Rect add(int x, int y, int w, int h, String label, String[] icon, Runnable action) {
        Rect r = new Rect();
        r.icon = icon;
        r.x = x;
        r.y = y;
        r.w = w;
        r.h = h;
        r.label = label;
        r.action = action;
        rects.add(r);
        return r;
    }

    private void layout() {
        rects.clear();
        int margin = Math.max(8, width / 30);
        panelW = Math.max(110, Math.min(190, (int) (width * 0.32f)));
        panelX = width - panelW - margin;

        // заголовок подгоняем под ширину панели
        titleScale = Math.max(1.0f, Math.min(3.0f, (panelW - 4) / (float) textRenderer.getWidth("HALLOWEEN")));
        titleY = 36;
        int y0 = titleY + (int) (8 * titleScale) + 18;

        boolean hasAccount = AccountSwitchers.available();
        int rows = hasAccount ? 6 : 5;          // ИГРАТЬ, Сеть, [Аккаунт], Моды, Настройки|Выход, Ванильное
        int bigH = 30, h = 22, gap = 5, accText = 14;
        int total = bigH + (rows - 1) * h + (rows - 1) * gap + 4 + accText;
        int avail = height - y0 - 6;
        if (total > avail) {
            float f = Math.max(0.6f, avail / (float) total);
            bigH = Math.max(20, (int) (bigH * f));
            h = Math.max(14, (int) (h * f));
            gap = Math.max(3, (int) (gap * f));
        }

        int y = y0;
        add(panelX, y, panelW, bigH, "ИГРАТЬ", null, () -> open(new SelectWorldScreen(this))).big = true;
        y += bigH + gap + 4;
        add(panelX, y, panelW, h, "Сетевая игра", BAT, () -> open(new MultiplayerScreen(this)));
        y += h + gap;
        if (hasAccount) {
            add(panelX, y, panelW, h, "Сменить аккаунт", GHOST, this::openAccounts);
            y += h + gap;
        }
        add(panelX, y, panelW, h, "Моды", SKULL, this::openMods);
        y += h + gap;
        int half = (panelW - gap) / 2;
        add(panelX, y, half, h, "Настройки", null, () -> open(new OptionsScreen(this, client.options)));
        add(panelX + half + gap, y, panelW - half - gap, h, "Выход", null, () -> client.scheduleStop());
        y += h + gap;
        add(panelX, y, panelW, h, "Ванильное меню", null, () -> {
            HalloweenState.vanilla = true;
            client.setScreen(new TitleScreen());
        });
        accountY = y + h + 4;

        int mw = 214;
        int mx = (width - mw) / 2;
        prevBtn = add(mx + 4, 6, 22, 20, "<", null, HalloweenMusic::previous);
        playBtn = add(mx + 28, 6, 22, 20, "||", null, HalloweenMusic::togglePause);
        nextBtn = add(mx + mw - 26, 6, 22, 20, ">", null, HalloweenMusic::next);
    }

    private void openAccounts() {
        Screen s = AccountSwitchers.create(this);
        if (s != null) {
            client.setScreen(s);
        } else {
            notice = "Не удалось открыть смену аккаунта";
            noticeUntil = System.currentTimeMillis() + 3500;
        }
    }

    private void open(Screen screen) {
        client.setScreen(screen);
    }

    private void openMods() {
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            try {
                Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                Screen s = (Screen) c.getConstructor(Screen.class).newInstance(this);
                client.setScreen(s);
                return;
            } catch (Throwable ignored) {
                // падаем в сообщение ниже
            }
        }
        notice = "Для кнопки «Моды» установи Mod Menu";
        noticeUntil = System.currentTimeMillis() + 3500;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Rect r : rects) {
                if (r.contains(mouseX, mouseY)) {
                    client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    r.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------------ рендер

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        double t = (System.currentTimeMillis() - start) / 1000.0 + 1.0;

        // 3D-сцена
        McPainter.begin(ctx);
        McPainter p = new McPainter(ctx);
        scene.render(p, width, height, t, mouseX / (float) width, mouseY / (float) height);
        p.flush();
        McPainter.end();

        AmbientFx.render(ctx, width, height, t);
        BigPumpkin.render(ctx, panelX + panelW / 2, accountY + 18, height - 8, panelW, t);

        drawTitle(ctx, t);

        for (Rect r : rects) {
            if (r == prevBtn || r == playBtn || r == nextBtn) {
                // нарисуем вместе с панелью плеера ниже
                continue;
            }
            drawOrange(ctx, r, r.contains(mouseX, mouseY), t);
        }
        drawPlayer(ctx, mouseX, mouseY);

        // аккаунт
        String acc = "Аккаунт: " + client.getSession().getUsername();
        float accScale = Math.min(1.0f, panelW / (float) textRenderer.getWidth(acc));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(panelX, accountY, 0);
        ctx.getMatrices().scale(accScale, accScale, 1f);
        ctx.drawText(textRenderer, acc, 0, 0, 0xFF9A93B5, true);
        ctx.getMatrices().pop();

        if (System.currentTimeMillis() < noticeUntil) {
            int w = textRenderer.getWidth(notice);
            ctx.fill(width / 2 - w / 2 - 8, height - 60, width / 2 + w / 2 + 8, height - 40, 0xE0140A08);
            ctx.drawBorder(width / 2 - w / 2 - 8, height - 60, w + 16, 20, 0xFFFF9628);
            ctx.drawText(textRenderer, notice, width / 2 - w / 2, height - 54, 0xFFFFD9A0, true);
        }
    }

    private void drawTitle(DrawContext ctx, double t) {
        String title = "HALLOWEEN";
        float scale = titleScale;
        int cx = panelX + panelW / 2;
        int w = (int) (textRenderer.getWidth(title) * scale);
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 2.2);
        int orange = 0xFF000000 | ((int) (255 * pulse) << 16) | ((int) (140 * pulse) << 8) | 0x28;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx - w / 2f, titleY, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(textRenderer, title, 1, 1, 0xFF3A1060, false);
        ctx.drawText(textRenderer, title, 0, 0, orange, false);
        ctx.getMatrices().pop();
        String sub = "ночное меню";
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub) / 2, titleY + (int) (8 * titleScale) + 4, 0xFF8F86B8, true);
    }

    /** Все кнопки меню в одном хеллоуинском стиле: оранжевый градиент, свечение, иконка. */
    private void drawOrange(DrawContext ctx, Rect r, boolean hover, double t) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.6);
        int x = r.x + (hover && !r.big ? -3 : 0);
        int glow = r.big ? (int) ((hover ? 90 : 40) + 40 * pulse) : (hover ? 80 : 26);
        for (int i = 3; i >= 1; i--) {
            int a = glow / (i + 1);
            ctx.fill(x - i * 2, r.y - i * 2, x + r.w + i * 2, r.y + r.h + i * 2, (a << 24) | 0xFF8A1C);
        }
        int top = hover ? 0xFFFFB03C : 0xFFFF9626;
        int bottom = hover ? 0xFFE0640C : 0xFFC85A0A;
        ctx.fillGradient(x, r.y, x + r.w, r.y + r.h, top, bottom);
        ctx.drawBorder(x, r.y, r.w, r.h, 0xFFFFD890);
        ctx.fill(x + 1, r.y + 1, x + r.w - 1, r.y + 3, 0x55FFFFFF);

        String[] icon = r.big ? PUMPKIN : r.icon;
        int textX = x;
        int textW = r.w;
        if (icon != null) {
            int s = Math.max(1, Math.min(2, (r.h - 8) / icon.length));
            int iw = icon[0].length() * s;
            drawSprite(ctx, icon, x + 7, r.y + (r.h - icon.length * s) / 2, s);
            textX = x + 7 + iw + 2;
            textW = r.w - (textX - x) - 4;
        }
        float maxScale = r.big ? 1.6f : 1.0f;
        float scale = Math.min(maxScale, Math.max(0.5f, (textW - 4) / (float) textRenderer.getWidth(r.label)));
        float tw = textRenderer.getWidth(r.label) * scale;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(textX + (textW - tw) / 2f, r.y + (r.h - 8 * scale) / 2f + 1, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(textRenderer, r.label, 0, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
    }

    private static void drawSprite(DrawContext ctx, String[] sprite, int px, int py, int s) {
        for (int row = 0; row < sprite.length; row++) {
            for (int col = 0; col < sprite[row].length(); col++) {
                int color;
                switch (sprite[row].charAt(col)) {
                    case 'o':
                        color = 0xFFE87812;
                        break;
                    case 'k':
                        color = 0xFF4A1C00;
                        break;
                    case 's':
                        color = 0xFF4E7A22;
                        break;
                    default:
                        continue;
                }
                ctx.fill(px + col * s, py + row * s, px + col * s + s, py + row * s + s, color);
            }
        }
    }

    private void drawPlayer(DrawContext ctx, int mouseX, int mouseY) {
        int mw = 214;
        int mx = (width - mw) / 2;
        ctx.fill(mx, 4, mx + mw, 28, 0xCC0A0812);
        ctx.drawBorder(mx, 4, mw, 24, 0xFF463A5A);
        for (Rect r : new Rect[]{prevBtn, playBtn, nextBtn}) {
            boolean hover = r.contains(mouseX, mouseY);
            ctx.fill(r.x, r.y + 1, r.x + r.w, r.y + r.h - 1, hover ? 0xFF3A2410 : 0xFF181424);
            ctx.drawBorder(r.x, r.y + 1, r.w, r.h - 2, hover ? 0xFFFF9628 : 0xFF463A5A);
            String label = r == playBtn ? (HalloweenMusic.isPaused() ? ">" : "||") : r.label;
            ctx.drawCenteredTextWithShadow(textRenderer, label, r.x + r.w / 2, r.y + (r.h - 8) / 2, 0xFFFFFFFF);
        }
        String name = HalloweenMusic.currentName() + (HalloweenMusic.isPaused() ? " (пауза)" : "");
        int cx = mx + 54 + (mw - 54 - 30) / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, name, cx, 12, 0xFFFFC87A);
    }
}
