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

    private static final class Rect {
        int x, y, w, h;
        String label;
        Runnable action;
        boolean big;

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

    private Rect add(int x, int y, int w, int h, String label, Runnable action) {
        Rect r = new Rect();
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
        panelW = (int) (width * 0.236f);
        panelX = (int) (width * 0.745f);
        titleY = (int) (height * 0.30f);

        int h = Math.max(22, (int) (height * 0.045f));
        int gap = Math.max(5, (int) (height * 0.012f));

        int bigH = Math.max(38, (int) (height * 0.085f));
        int playW = (int) (width * 0.166f);
        add((int) (width * 0.567f), (int) (height * 0.46f), playW, bigH, "ИГРАТЬ", () -> open(new SelectWorldScreen(this))).big = true;

        int y = (int) (height * 0.44f);
        add(panelX, y, panelW, h, "Одиночная игра", () -> open(new SelectWorldScreen(this)));
        y += h + gap;
        add(panelX, y, panelW, h, "Сетевая игра", () -> open(new MultiplayerScreen(this)));
        y += h + gap + 6;
        add(panelX, y, panelW, h, "Моды", this::openMods);
        y += h + gap;
        int half = (panelW - gap) / 2;
        add(panelX, y, half, h, "Настройки", () -> open(new OptionsScreen(this, client.options)));
        add(panelX + half + gap, y, panelW - half - gap, h, "Выйти из игры", () -> client.scheduleStop());
        accountY = y + h + gap + 4;

        add(panelX, (int) (height * 0.835f), panelW, h, "Ванильное главное меню", () -> {
            HalloweenState.vanilla = true;
            client.setScreen(new TitleScreen());
        });

        int mw = 214;
        int mx = (width - mw) / 2;
        prevBtn = add(mx + 4, 6, 22, 20, "<", HalloweenMusic::previous);
        playBtn = add(mx + 28, 6, 22, 20, "||", HalloweenMusic::togglePause);
        nextBtn = add(mx + mw - 26, 6, 22, 20, ">", HalloweenMusic::next);
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

        drawTitle(ctx, t);

        for (Rect r : rects) {
            boolean hover = r.contains(mouseX, mouseY);
            if (r.big) {
                drawPlay(ctx, r, hover, t);
            } else if (r == prevBtn || r == playBtn || r == nextBtn) {
                // нарисуем вместе с панелью плеера ниже
                continue;
            } else {
                drawButton(ctx, r, hover);
            }
        }
        drawPlayer(ctx, mouseX, mouseY);

        // аккаунт
        String acc = "Текущий аккаунт: " + client.getSession().getUsername();
        ctx.drawText(textRenderer, acc, panelX, accountY, 0xFF9A93B5, true);

        if (System.currentTimeMillis() < noticeUntil) {
            int w = textRenderer.getWidth(notice);
            ctx.fill(width / 2 - w / 2 - 8, height - 60, width / 2 + w / 2 + 8, height - 40, 0xE0140A08);
            ctx.drawBorder(width / 2 - w / 2 - 8, height - 60, w + 16, 20, 0xFFFF9628);
            ctx.drawText(textRenderer, notice, width / 2 - w / 2, height - 54, 0xFFFFD9A0, true);
        }
    }

    private void drawTitle(DrawContext ctx, double t) {
        String title = "HALLOWEEN";
        float scale = 3.0f;
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
        ctx.drawText(textRenderer, sub, cx - textRenderer.getWidth(sub) / 2, titleY + 32, 0xFF8F86B8, true);
    }

    private void drawButton(DrawContext ctx, Rect r, boolean hover) {
        int bg = hover ? 0xE62E1A0C : 0xD70E0C16;
        int border = hover ? 0xFFFF9628 : 0xFF463A5A;
        int off = hover ? -3 : 0;
        int x = r.x + off;
        ctx.fill(x, r.y, x + r.w, r.y + r.h, bg);
        ctx.drawBorder(x, r.y, r.w, r.h, border);
        if (hover) {
            ctx.fill(x + 1, r.y + 1, x + r.w - 1, r.y + 3, 0x40FFAA40);
        }
        int color = hover ? 0xFFFFFFFF : 0xFFD2CDE4;
        ctx.drawCenteredTextWithShadow(textRenderer, r.label, x + r.w / 2, r.y + (r.h - 8) / 2, color);
    }

    private void drawPlay(DrawContext ctx, Rect r, boolean hover, double t) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.6);
        int glow = (int) ((hover ? 90 : 40) + 40 * pulse);
        for (int i = 4; i >= 1; i--) {
            int a = glow / (i + 1);
            ctx.fill(r.x - i * 2, r.y - i * 2, r.x + r.w + i * 2, r.y + r.h + i * 2, (a << 24) | 0xFF8A1C);
        }
        int top = hover ? 0xFFFFB03C : 0xFFFF9626;
        int bottom = hover ? 0xFFE0640C : 0xFFC85A0A;
        ctx.fillGradient(r.x, r.y, r.x + r.w, r.y + r.h, top, bottom);
        ctx.drawBorder(r.x, r.y, r.w, r.h, 0xFFFFD890);
        ctx.fill(r.x + 1, r.y + 1, r.x + r.w - 1, r.y + 3, 0x55FFFFFF);

        // тыква
        int px = r.x + 8, py = r.y + 8, s = 2;
        for (int row = 0; row < PUMPKIN.length; row++) {
            for (int col = 0; col < PUMPKIN[row].length(); col++) {
                char ch = PUMPKIN[row].charAt(col);
                int color;
                switch (ch) {
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
        // подпись
        String label = r.label;
        float scale = 1.6f;
        int tw = (int) (textRenderer.getWidth(label) * scale);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(r.x + 36 + (r.w - 36 - tw) / 2f, r.y + (r.h - 8 * scale) / 2f + 1, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawText(textRenderer, label, 0, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().pop();
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
