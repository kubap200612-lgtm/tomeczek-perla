package pl.pearltrail;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Ekran ustawień smugi (domyślnie klawisz O). */
public class SettingsScreen extends Screen {

    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Pearl Trail – ustawienia"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TrailConfig cfg = TrailConfig.INSTANCE;
        int w = 220;
        int x = this.width / 2 - w / 2;
        int y = this.height / 6;
        int step = 24;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.enabled)
                .build(x, y, w, 20, Text.literal("Smuga"), (b, v) -> cfg.enabled = v));
        y += step;

        addDrawableChild(new ConfigSlider(x, y, w, 20, 0.05, 1.0, cfg.density,
                v -> "Gęstość smugi: " + Math.round(v * 100) + "%",
                v -> cfg.density = (float) v, 0.05));
        y += step;

        addDrawableChild(new ConfigSlider(x, y, w, 20, 0.0, 10.0, cfg.fadeSeconds,
                v -> String.format("Czas smugi po uderzeniu: %.1f s", v),
                v -> cfg.fadeSeconds = (float) v, 0.1));
        y += step;

        addDrawableChild(new ConfigSlider(x, y, w, 20, 0, 150, cfg.burstParticles,
                v -> "Wybuch przy uderzeniu: " + Math.round(v) + " cząsteczek",
                v -> cfg.burstParticles = (int) Math.round(v), 1));
        y += step + 8;

        addDrawableChild(CyclingButtonWidget.onOffBuilder(cfg.highlight)
                .build(x, y, w, 20, Text.literal("Podświetlenie bloku"), (b, v) -> cfg.highlight = v));
        y += step;

        addDrawableChild(new ConfigSlider(x, y, w, 20, 1.0, 15.0, cfg.highlightSeconds,
                v -> String.format("Czas podświetlenia: %.1f s", v),
                v -> cfg.highlightSeconds = (float) v, 0.5));
        y += step;

        addDrawableChild(CyclingButtonWidget.<TrailConfig.HighlightColor>builder(c -> Text.literal(c.label).withColor(c.rgb))
                .values(TrailConfig.HighlightColor.values())
                .initially(cfg.highlightColor)
                .build(x, y, w, 20, Text.literal("Kolor podświetlenia"), (b, v) -> cfg.highlightColor = v));
        y += step + 12;

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(x, y, w, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 6 - 18, 0xB04CFF);
    }

    @Override
    public void close() {
        TrailConfig.save();
        if (this.client != null) this.client.setScreen(parent);
    }

    /** Suwak z zakresem min-max i zaokrąglaniem do kroku. */
    private static class ConfigSlider extends SliderWidget {
        private final double min, max, step;
        private final DoubleFunction<String> label;
        private final DoubleConsumer setter;

        ConfigSlider(int x, int y, int w, int h, double min, double max, double current,
                     DoubleFunction<String> label, DoubleConsumer setter, double step) {
            super(x, y, w, h, Text.empty(), MathHelper.clamp((current - min) / (max - min), 0, 1));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.setter = setter;
            updateMessage();
        }

        private double real() {
            double v = min + value * (max - min);
            return Math.round(v / step) * step;
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label.apply(real())));
        }

        @Override
        protected void applyValue() {
            setter.accept(real());
        }
    }
}
