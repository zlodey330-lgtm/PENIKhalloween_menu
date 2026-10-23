package com.halloweenmenu.client.mixin;

import com.halloweenmenu.client.ThemedBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {

    @Shadow
    protected double value;

    /** Ползунки (громкость, чувствительность, дальность прорисовки) в хеллоуинском стиле. */
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void halloween$slider(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ThemedBackground.applies(client.currentScreen)) {
            return;
        }
        SliderWidget s = (SliderWidget) (Object) this;
        int x = s.getX();
        int y = s.getY();
        int w = s.getWidth();
        int h = s.getHeight();
        boolean hover = s.active && (s.isHovered() || s.isFocused());

        context.fill(x, y, x + w, y + h, s.active ? 0xD70E0C16 : 0x900E0C16);
        context.drawBorder(x, y, w, h, hover ? 0xFFFF9628 : 0xFF463A5A);

        double v = Math.max(0.0, Math.min(1.0, this.value));
        int handleW = 8;
        int hx = x + (int) (v * (w - handleW));
        // заполненная часть
        context.fillGradient(x + 1, y + 1, hx + 2, y + h - 1, 0x80C85A0A, 0x80FF9626);
        // ручка
        context.fillGradient(hx, y, hx + handleW, y + h, hover ? 0xFFFFB03C : 0xFFFF9626, hover ? 0xFFE0640C : 0xFFC85A0A);
        context.drawBorder(hx, y, handleW, h, 0xFFFFD890);

        int color = !s.active ? 0xFF6E6A7A : 0xFFFFFFFF;
        context.drawCenteredTextWithShadow(client.textRenderer, s.getMessage(), x + w / 2, y + (h - 8) / 2, color);
        ci.cancel();
    }
}
