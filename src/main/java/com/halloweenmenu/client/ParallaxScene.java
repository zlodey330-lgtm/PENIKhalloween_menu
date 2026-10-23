package com.halloweenmenu.client;

/**
 * Живая 2.5D-сцена из картинки: сетка с глубиной (параллакс от камеры и мыши),
 * парящая ведьма, мерцающие фонари, пульсирующий портал и луна, летучие мыши,
 * плывущий туман, искры и редкие вспышки молнии.
 * Класс не зависит от Minecraft: рисует через {@link Painter}.
 */
public final class ParallaxScene {

    /** Куда рисуем. Цвета в формате ARGB. */
    public interface Painter {
        /** Четырёхугольник с текстурой сцены; uv в долях 0..1. */
        void texQuad(float[] xy, float[] uv, int argb);

        void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb);

        void vgrad(float x1, float y1, float x2, float y2, int topArgb, int bottomArgb);

        void hgrad(float x1, float y1, float x2, float y2, int leftArgb, int rightArgb);

        void disc(float cx, float cy, float rx, float ry, int centerArgb, int edgeArgb, boolean additive);
    }

    private static final float IW = 1672f, IH = 941f;
    private static final int GX = SceneData.GX, GY = SceneData.GY;

    private final float[] depth = SceneData.decode(SceneData.DEPTH);
    private final float[] mask = SceneData.decode(SceneData.MASK);

    // текущие параметры кадра
    private float scale, offX, offY, cam, camY;
    private double time;

    private final float[] xy = new float[8];
    private final float[] uv = new float[8];

    private static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (r << 16) | (g << 8) | b;
    }

    private float sx(float ix, float d) {
        return offX + ix * scale + (d - 0.35f) * cam * 22f * scale;
    }

    private float sy(float iy, float d) {
        return offY + iy * scale + (d - 0.35f) * camY * 12f * scale;
    }

    /** Точка картинки -> экран с учётом глубины. */
    private float ax(float ix, float iy, float d) {
        return sx(ix, d);
    }

    private float ay(float ix, float iy, float d) {
        return sy(iy, d);
    }

    private float bobX(float m) {
        return (float) Math.sin(time * 0.9) * 4f * scale * m;
    }

    private float bobY(float m) {
        return (float) (Math.sin(time * 1.5) * 9f + Math.sin(time * 2.9) * 2f) * scale * m;
    }

    public void render(Painter p, float W, float H, double t, float mx, float my) {
        time = t;
        scale = Math.max(W / IW, H / IH) * (1.04f + 0.01f * (float) Math.sin(t * 0.22));
        offX = (W - IW * scale) / 2f;
        offY = (H - IH * scale) / 2f;
        cam = (float) Math.sin(t * 0.31) * 0.6f + (mx - 0.5f) * 1.8f;
        camY = (float) Math.sin(t * 0.27 + 1.0) * 0.5f + (my - 0.5f) * 1.0f;

        p.quad(0, 0, W, 0, W, H, 0, H, 0xFF05060C);

        // ---- сетка с картинкой
        float breathe = 0.97f + 0.03f * (float) Math.sin(t * 1.3);
        int c = (int) (255 * breathe);
        int tint = argb(255, c, c, c);
        int stride = GX + 1;
        for (int j = 0; j < GY; j++) {
            for (int i = 0; i < GX; i++) {
                int k0 = j * stride + i, k1 = k0 + 1, k2 = k0 + stride + 1, k3 = k0 + stride;
                put(0, i, j, k0);
                put(1, i + 1, j, k1);
                put(2, i + 1, j + 1, k2);
                put(3, i, j + 1, k3);
                p.texQuad(xy, uv, tint);
            }
        }

        // ---- мягкая подсветка (additive)
        // луна
        float moonPulse = 0.85f + 0.15f * (float) Math.sin(t * 0.8);
        glow(p, 400, 203, 0.1f, 330, argb((int) (34 * moonPulse), 150, 170, 255));
        glow(p, 400, 203, 0.1f, 150, argb((int) (46 * moonPulse), 200, 215, 255));

        // фонари и огоньки: x, y, глубина, радиус, r,g,b, фаза
        float[][] lamps = {
                {172, 432, 0.85f, 90, 255, 160, 60, 0.0f},
                {72, 495, 0.85f, 70, 90, 230, 220, 1.3f},
                {280, 580, 0.9f, 130, 110, 200, 255, 2.1f},
                {400, 640, 0.6f, 70, 255, 170, 70, 0.7f},
                {347, 696, 0.9f, 60, 100, 220, 255, 3.2f},
                {688, 642, 0.4f, 80, 255, 150, 60, 1.9f},
                {828, 632, 0.55f, 85, 255, 150, 60, 4.4f},
                {890, 553, 0.55f, 70, 255, 170, 70, 5.1f},
                {45, 625, 0.9f, 90, 255, 170, 60, 2.7f},
        };
        for (float[] l : lamps) {
            float fl = 0.78f + 0.22f * (float) Math.sin(t * 6.3 + l[7]) * (float) Math.sin(t * 2.1 + l[7] * 2f)
                    + 0.08f * (float) Math.sin(t * 13 + l[7]);
            glow(p, l[0], l[1], l[2], l[3] * (0.9f + 0.1f * fl), argb((int) (95 * fl), (int) l[4], (int) l[5], (int) l[6]));
        }
        // портал
        float pp = 0.7f + 0.3f * (float) Math.sin(t * 2.4);
        glow(p, 748, 610, 0.4f, 150 * (0.9f + 0.1f * pp), argb((int) (90 * pp), 170, 80, 255));
        glow(p, 748, 640, 0.4f, 60, argb((int) (70 * pp), 210, 140, 255));

        // ведьма: глаз и шары в руках
        float bx = bobX(1), by = bobY(1);
        float wd = 0.82f;
        float eye = 0.6f + 0.4f * (float) Math.sin(t * 3.0);
        glowAt(p, ax(568, 458, wd) + bx, ay(568, 458, wd) + by, 34 * scale, argb((int) (110 * eye), 60, 255, 180));
        float o1 = 0.7f + 0.3f * (float) Math.sin(t * 4.2);
        float o2 = 0.7f + 0.3f * (float) Math.sin(t * 3.6 + 1.1);
        glowAt(p, ax(352, 582, wd) + bx, ay(352, 582, wd) + by, 90 * scale, argb((int) (95 * o1), 90, 210, 255));
        glowAt(p, ax(612, 614, wd) + bx, ay(612, 614, wd) + by, 70 * scale, argb((int) (95 * o2), 255, 175, 70));

        // ---- туман
        for (int k = 0; k < 7; k++) {
            float sp = 14f + k * 4f;
            float x = (float) (((t * sp + k * 260) % (IW * 0.75 + 800)) - 400);
            float y = 560 + k * 36 + (float) Math.sin(t * 0.4 + k) * 14;
            float d = 0.5f;
            float cx = ax(x, y, d), cy = ay(x, y, d);
            p.disc(cx, cy, 420 * scale, 64 * scale, argb(22, 140, 155, 220), argb(0, 140, 155, 220), false);
        }

        // ---- летучие мыши
        for (int k = 0; k < 5; k++) {
            float bxk = 450 + (float) Math.sin(t * (0.17 + 0.04 * k) + k * 2.1) * 430f;
            float byk = 100 + k * 42 + (float) Math.sin(t * 0.8 + k * 1.7) * 28f;
            float size = 15 + k * 4;
            float dir = (float) Math.cos(t * (0.17 + 0.04 * k) + k * 2.1) >= 0 ? 1f : -1f;
            bat(p, ax(bxk, byk, 0.1f), ay(bxk, byk, 0.1f), size * scale, (float) Math.sin(t * (11 + k) + k), dir);
        }

        // ---- искры
        for (int k = 0; k < 70; k++) {
            float seed = k * 12.9898f;
            float baseX = 30 + frac(seed * 3.1f) * 860f;
            float speed = 0.04f + frac(seed * 7.7f) * 0.07f;
            float life = (float) ((t * speed + frac(seed)) % 1.0);
            float x = baseX + (float) Math.sin(t * 0.9 + k) * 18f;
            float y = 900 - life * 640f;
            float d = 0.3f + frac(seed * 2.3f) * 0.6f;
            float a = (float) Math.sin(life * Math.PI) * (0.55f + 0.45f * (float) Math.sin(t * 5 + k));
            int type = k % 5;
            int r = type == 0 ? 120 : (type == 1 ? 190 : 255);
            int g = type == 0 ? 230 : (type == 1 ? 120 : 175);
            int b = type == 0 ? 255 : (type == 1 ? 255 : 70);
            float rad = (1.6f + frac(seed * 5.1f) * 2.6f) * scale;
            float cx = ax(x, y, d), cy = ay(x, y, d);
            p.disc(cx, cy, rad * 3f, rad * 3f, argb((int) (150 * a), r, g, b), argb(0, r, g, b), true);
            p.disc(cx, cy, rad * 0.8f, rad * 0.8f, argb((int) (230 * a), 255, 255, 255), argb((int) (200 * a), r, g, b), true);
        }

        // ---- редкая вспышка молнии по облакам
        double ph = t % 17.0;
        float flash = 0f;
        if (ph < 0.15) {
            flash = (float) (ph / 0.15);
        } else if (ph < 0.9) {
            flash = (float) Math.exp(-(ph - 0.15) * 5.0);
        }
        if (ph > 1.1 && ph < 1.3) {
            flash = Math.max(flash, 0.5f * (float) Math.sin((ph - 1.1) / 0.2 * Math.PI));
        }
        if (flash > 0.01f) {
            p.vgrad(0, 0, W, H * 0.6f, argb((int) (50 * flash), 170, 190, 255), argb(0, 170, 190, 255));
        }

        // ---- виньетка
        p.vgrad(0, 0, W, H * 0.18f, 0x66000000, 0x00000000);
        p.vgrad(0, H * 0.78f, W, H, 0x00000000, 0x77000000);
        p.hgrad(0, 0, W * 0.1f, H, 0x66000000, 0x00000000);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private void put(int n, int i, int j, int k) {
        float ix = i * IW / GX, iy = j * IH / GY;
        float d = depth[k], m = mask[k];
        xy[n * 2] = sx(ix, d) + bobX(m);
        xy[n * 2 + 1] = sy(iy, d) + bobY(m);
        uv[n * 2] = i / (float) GX;
        uv[n * 2 + 1] = j / (float) GY;
    }

    private void glow(Painter p, float ix, float iy, float d, float rImg, int center) {
        glowAt(p, ax(ix, iy, d), ay(ix, iy, d), rImg * scale, center);
    }

    private void glowAt(Painter p, float cx, float cy, float r, int center) {
        int edge = center & 0x00FFFFFF;
        p.disc(cx, cy, r, r, center, edge, true);
    }

    private void bat(Painter p, float cx, float cy, float s, float flap, float dir) {
        int col = 0xF2090912;
        for (int side = -1; side <= 1; side += 2) {
            float w = s * 1.5f * side;
            p.quad(cx + w * 0.1f, cy - s * 0.15f,
                    cx + w, cy - flap * s * 0.9f,
                    cx + w * 0.78f, cy - flap * s * 0.2f + s * 0.45f,
                    cx + w * 0.25f, cy + s * 0.32f, col);
            p.quad(cx + w, cy - flap * s * 0.9f,
                    cx + w * 1.12f, cy - flap * s * 0.9f + s * 0.3f,
                    cx + w * 0.78f, cy - flap * s * 0.2f + s * 0.45f,
                    cx + w * 0.9f, cy - flap * s * 0.55f, col);
        }
        p.disc(cx, cy + s * 0.05f, s * 0.28f, s * 0.38f, col, col, false);
        p.quad(cx - s * 0.2f, cy - s * 0.3f, cx - s * 0.1f, cy - s * 0.62f, cx, cy - s * 0.3f, cx, cy - s * 0.3f, col);
        p.quad(cx + s * 0.2f, cy - s * 0.3f, cx + s * 0.1f, cy - s * 0.62f, cx, cy - s * 0.3f, cx, cy - s * 0.3f, col);
    }
}
