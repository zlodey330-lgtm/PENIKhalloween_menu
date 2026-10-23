package com.halloweenmenu.client;

import net.minecraft.client.gui.DrawContext;

/**
 * Живой фон для чёрной правой части меню: поднимающиеся искры и плывущие
 * тыквы, привидения и летучие мыши. Всё считается от времени, без состояния.
 */
final class AmbientFx {

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

    private static final String[] GHOST = {
            ".wwwww.",
            "wwwwwww",
            "wkwwwkw",
            "wkwwwkw",
            "wwwkwww",
            "wwwwwww",
            "wwwwwww",
            "w.www.w"
    };

    private static final String[] BAT_UP = {
            "p.......p",
            "pp.ppp.pp",
            "ppppppppp",
            "ppppppppp",
            ".pp.p.pp.",
            "..p...p.."
    };

    private static final String[] BAT_DOWN = {
            "...ppp...",
            "..ppppp..",
            "ppppppppp",
            "pp.ppp.pp",
            "p...p...p"
    };

    private static final int EMBERS = 36;
    private static final int SPRITES = 8;

    private AmbientFx() {
    }

    private static float rnd(int i, int k) {
        int x = i * 374761393 + k * 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= x >>> 16;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float frac(double v) {
        return (float) (v - Math.floor(v));
    }

    static void render(DrawContext ctx, int w, int h, double t) {
        int x0 = (int) (w * 0.45f);
        int x1 = w;
        int span = x1 - x0;

        // лёгкий туман у нижнего края
        ctx.fillGradient(x0, h - h / 3, x1, h, 0x00503080, 0x38503080);

        // плывущие тыквы, привидения, мыши
        for (int i = 0; i < SPRITES; i++) {
            int type = i % 3;
            float period = (type == 2 ? 11f : 17f) + rnd(i, 1) * 9f;
            float u = frac(t / period + rnd(i, 2));
            float fade = Math.min(1f, (float) Math.sin(Math.PI * u) * 2f);
            if (type == 2) {
                int frame = ((int) (t * 6 + i)) % 2;
                float x = x1 + 12 - u * (span + 40);
                float y = h * (0.40f + 0.5f * rnd(i, 3)) + (float) Math.sin(t * 1.3 + rnd(i, 4) * 6.28) * 12f;
                sprite(ctx, frame == 0 ? BAT_UP : BAT_DOWN, x, y, 2, fade * 0.65f);
            } else {
                String[] spr = type == 0 ? PUMPKIN : GHOST;
                float x = x0 + 10 + rnd(i, 3) * (span - 50) + (float) Math.sin(t * 0.6 + rnd(i, 4) * 6.28) * 10f;
                float y = h + 10 - u * (h + 40);
                sprite(ctx, spr, x, y, 2, fade * 0.45f);
            }
        }

        // искры
        for (int i = 0; i < EMBERS; i++) {
            float period = 7f + rnd(i, 1) * 8f;
            float u = frac(t / period + rnd(i, 2));
            float x = x0 + rnd(i, 3) * span + (float) Math.sin(t * 0.7 + rnd(i, 4) * 6.28) * 6f;
            float y = h + 4 - u * (h * 0.9f);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * 4 + i);
            float a = (float) Math.sin(Math.PI * u) * (0.45f + 0.55f * twinkle);
            int size = rnd(i, 5) > 0.7f ? 2 : 1;
            int rgb = rnd(i, 6) < 0.65f ? 0xFF9A2A : 0xB06AFF;
            int ix = (int) x;
            int iy = (int) y;
            ctx.fill(ix - 1, iy - 1, ix + size + 1, iy + size + 1, alpha(a * 0.22f) | rgb);
            ctx.fill(ix, iy, ix + size, iy + size, alpha(a) | rgb);
        }
    }

    private static int alpha(float a) {
        return Math.max(0, Math.min(255, (int) (a * 255))) << 24;
    }

    private static void sprite(DrawContext ctx, String[] sprite, float fx, float fy, int s, float a) {
        int px = (int) fx;
        int py = (int) fy;
        int al = alpha(a);
        for (int row = 0; row < sprite.length; row++) {
            for (int col = 0; col < sprite[row].length(); col++) {
                int rgb;
                switch (sprite[row].charAt(col)) {
                    case 'o':
                        rgb = 0xE87812;
                        break;
                    case 'k':
                        rgb = 0x4A1C00;
                        break;
                    case 's':
                        rgb = 0x4E7A22;
                        break;
                    case 'w':
                        rgb = 0xDCD2F0;
                        break;
                    case 'p':
                        rgb = 0x6A44A0;
                        break;
                    default:
                        continue;
                }
                if (sprite == GHOST && sprite[row].charAt(col) == 'k') {
                    rgb = 0x2A1040;
                }
                ctx.fill(px + col * s, py + row * s, px + col * s + s, py + row * s + s, al | rgb);
            }
        }
    }
}
