package com.halloweenmenu.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/**
 * Рисует примитивы ParallaxScene прямо в GUI Майнкрафта (цветные треугольники/четырёхугольники).
 * Соседние вызовы одного типа склеиваются в один draw-call.
 */
final class McPainter implements ParallaxScene.Painter {

    static final Identifier SCENE_TEXTURE = Identifier.of("halloween_menu", "textures/gui/scene.png");

    private static final int SEGMENTS = 20;

    private enum Kind { QUADS, DISC, DISC_ADD, TEX, NONE }

    private final Matrix4f matrix;
    private BufferBuilder builder;
    private Kind kind = Kind.NONE;

    McPainter(DrawContext context) {
        this(context, 0);
    }

    /** mirrorWidth > 0: сцена зеркально отражается по горизонтали на экране такой ширины. */
    McPainter(DrawContext context, int mirrorWidth) {
        Matrix4f m = new Matrix4f(context.getMatrices().peek().getPositionMatrix());
        if (mirrorWidth > 0) {
            m.translate((float) mirrorWidth, 0f, 0f);
            m.scale(-1f, 1f, 1f);
        }
        this.matrix = m;
    }

    static void begin(DrawContext context) {
        context.draw();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
    }

    static void end() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private void use(Kind wanted) {
        if (kind == wanted) {
            return;
        }
        flush();
        kind = wanted;
        if (wanted == Kind.TEX) {
            builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            return;
        }
        VertexFormat.DrawMode mode = wanted == Kind.QUADS ? VertexFormat.DrawMode.QUADS : VertexFormat.DrawMode.TRIANGLES;
        builder = Tessellator.getInstance().begin(mode, VertexFormats.POSITION_COLOR);
    }

    void flush() {
        if (builder != null) {
            if (kind == Kind.DISC_ADD) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            } else {
                RenderSystem.defaultBlendFunc();
            }
            if (kind == Kind.TEX) {
                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                RenderSystem.setShaderTexture(0, SCENE_TEXTURE);
                BufferRenderer.drawWithGlobalProgram(builder.end());
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            } else {
                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
            builder = null;
        }
        kind = Kind.NONE;
    }

    private void v(float x, float y, int argb) {
        builder.vertex(matrix, x, y, 0f).color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
    }

    @Override
    public void texQuad(float[] xy, float[] uv, int argb) {
        use(Kind.TEX);
        for (int i = 0; i < 4; i++) {
            builder.vertex(matrix, xy[i * 2], xy[i * 2 + 1], 0f)
                    .texture(uv[i * 2], uv[i * 2 + 1])
                    .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
        }
    }

    @Override
    public void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int argb) {
        use(Kind.QUADS);
        v(x1, y1, argb);
        v(x2, y2, argb);
        v(x3, y3, argb);
        v(x4, y4, argb);
    }

    @Override
    public void vgrad(float x1, float y1, float x2, float y2, int top, int bottom) {
        use(Kind.QUADS);
        v(x1, y1, top);
        v(x1, y2, bottom);
        v(x2, y2, bottom);
        v(x2, y1, top);
    }

    @Override
    public void hgrad(float x1, float y1, float x2, float y2, int left, int right) {
        use(Kind.QUADS);
        v(x1, y1, left);
        v(x1, y2, left);
        v(x2, y2, right);
        v(x2, y1, right);
    }

    @Override
    public void disc(float cx, float cy, float rx, float ry, int center, int edge, boolean additive) {
        if (rx < 0.5f || ry < 0.5f) {
            return;
        }
        use(additive ? Kind.DISC_ADD : Kind.DISC);
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = i * Math.PI * 2 / SEGMENTS;
            double a1 = (i + 1) * Math.PI * 2 / SEGMENTS;
            v(cx, cy, center);
            v(cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry, edge);
            v(cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, edge);
        }
    }
}
