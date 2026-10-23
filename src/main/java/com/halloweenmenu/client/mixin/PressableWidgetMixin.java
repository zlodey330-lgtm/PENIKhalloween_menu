package com.halloweenmenu.client.mixin;

import com.halloweenmenu.client.ThemedBackground;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {

    /** Рисует кнопки в хеллоуинском стиле (чёрно-фиолетовые с оранжевой рамкой при наведении). */
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void halloween$button(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object self = this;
        if (!(self instanceof ButtonWidget) && !(self instanceof CyclingButtonWidget)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ThemedBackground.applies(client.currentScreen)) {
            return;
        }
        PressableWidget b = (PressableWidget) self;
        int x = b.getX();
        int y = b.getY();
        int w = b.getWidth();
        int h = b.getHeight();
        boolean active = b.active;
        boolean hover = active && (b.isHovered() || b.isFocused());

        int bg = !active ? 0x900E0C16 : hover ? 0xE62E1A0C : 0xD70E0C16;
        int border = !active ? 0xFF2A2536 : hover ? 0xFFFF9628 : 0xFF463A5A;
        context.fill(x, y, x + w, y + h, bg);
        context.drawBorder(x, y, w, h, border);
        if (hover) {
            context.fill(x + 1, y + 1, x + w - 1, y + 3, 0x40FFAA40);
        }
        int color = !active ? 0xFF6E6A7A : hover ? 0xFFFFFFFF : 0xFFD2CDE4;
        context.drawCenteredTextWithShadow(client.textRenderer, b.getMessage(), x + w / 2, y + (h - 8) / 2, color);
        ci.cancel();
    }
}
