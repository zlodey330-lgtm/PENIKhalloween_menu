package com.halloweenmenu.client;

import net.minecraft.client.gui.DrawContext;

/** Большая светящаяся тыква-фонарь в правом нижнем углу меню. */
final class BigPumpkin {

    private static final int GW = 22;
    private static final int GH = 18;

    private static final String[] FACE = {
            "..xx..........xx..",
            ".xxxx........xxxx.",
            "xxxxxx......xxxxxx",
            ".xxxx...xx...xxxx.",
            "..xx...xxxx...xx..",
            "..................",
            "x.xxxxxxxxxxxxxx.x",
            "xxxxxxxxxxxxxxxxxx",
            ".xx.xxxxxxxxxx.xx.",
            "..x..xx.xxxx.xx.x."
    };

    /** 0 = пусто, иначе ARGB цвета клетки. */
    private static final int[][] BODY = new int[GH][GW];
    private static final boolean[][] LIT = new boolean[GH][GW];

    static {
        double cx0 = GW / 2.0;
        double cy0 = GH / 2.0 + 1.5;
        for (int gy = 0; gy < GH; gy++) {
            for (int gx = 0; gx < GW; gx++) {
                double nx = (gx + 0.5 - cx0) / (GW / 2.0);
                double ny = (gy + 0.5 - cy0) / (GH / 2.0 - 1.2);
                if (nx * nx + ny * ny <= 1.0) {
                    double rib = 0.5 + 0.5 * Math.cos((gx + 0.5 - cx0) * 0.95);
                    double t = 1 - rib;
                    int r = (int) (236 + (176 - 236) * t);
                    int g = (int) (118 + (72 - 118) * t);
                    int b = (int) (18 + (8 - 18) * t);
                    if (ny < -0.55) {
                        r = Math.min(255, r + 22);
                        g = Math.min(255, g + 22);
                        b = Math.min(255, b + 22);
                    }
                    if (ny > 0.7) {
                        r = (int) (r * 0.8);
                        g = (int) (g * 0.8);
                        b = (int) (b * 0.8);
                    }
                    BODY[gy][gx] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        int[][] stem = {{10, 0}, {11, 0}, {10, 1}, {11, 1}, {12, 1}, {10, 2}, {11, 2}, {12, 2}};
        for (int[] c : stem) {
            BODY[c[1]][c[0]] = c[1] < 2 ? 0xFF4E7A22 : 0xFF3C641A;
        }
        for (int r = 0; r < FACE.length; r++) {
            for (int c = 0; c < FACE[r].length(); c++) {
                int gx = c + 2;
                int gy = r + 5;
                if (FACE[r].charAt(c) == 'x' && gy < GH && gx < GW && BODY[gy][gx] != 0) {
                    LIT[gy][gx] = true;
                }
            }
        }
    }

    private BigPumpkin() {
    }

    /**
     * @param cx      центр по горизонтали
     * @param top     верхняя граница свободного места
     * @param bottom  нижняя граница свободного места
     * @param availW  доступная ширина
     */
    static void render(DrawContext ctx, int cx, int top, int bottom, int availW, double t) {
        int availH = bottom - top;
        int cell = Math.min(8, Math.min(availW / GW, availH / GH));
        if (cell < 2) {
            return; // окно слишком маленькое
        }
        int pw = GW * cell;
        int ph = GH * cell;
        int ox = cx - pw / 2;
        int oy = top + (availH - ph) / 2;

        // мерцание свечи внутри
        double flick = 0.82 + 0.12 * Math.sin(t * 7.0 + Math.sin(t * 13.0)) + 0.06 * Math.sin(t * 19.0);
        float f = (float) Math.max(0.6, Math.min(1.0, flick));

        // тёплое свечение за тыквой
        McPainter.begin(ctx);
        McPainter painter = new McPainter(ctx);
        int glowA = (int) (110 * f);
        painter.disc(cx, oy + ph * 0.55f, pw * 0.85f, ph * 0.85f, (glowA << 24) | 0xFF7A14, 0x00FF7A14, true);
        painter.flush();
        McPainter.end();

        for (int gy = 0; gy < GH; gy++) {
            for (int gx = 0; gx < GW; gx++) {
                int color = BODY[gy][gx];
                if (color == 0) {
                    continue;
                }
                if (LIT[gy][gx]) {
                    int r = (int) (255 * f);
                    int g = (int) (214 * f);
                    int b = (int) (90 * f);
                    color = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                int x = ox + gx * cell;
                int y = oy + gy * cell;
                ctx.fill(x, y, x + cell, y + cell, color);
            }
        }
    }
}
