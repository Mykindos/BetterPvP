package me.mykindos.betterpvp.core.utilities.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.awt.*;

public class ProgressColor {

    private final float progress; // Scales from 0 to 1.0
    private boolean inverted = false;

    public ProgressColor(float progress) {
        this.progress = progress;
    }

    public static ProgressColor of(float progress){
        return new ProgressColor(progress);
    }

    public TextComponent withText(final String text) {
        return Component.text().color(this.getTextColor()).content(text).build();
    }

    public TextColor getTextColor() {
        final Color color = this.getColor();
        return TextColor.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    public float getProgress() {
        return this.progress;
    }

    /**
     * If inverted, the color will go from green to red. Otherwise, it will go from red to green.
     */
    public ProgressColor inverted() {
        this.inverted = true;
        return this;
    }

    public Color getColor() {
        final float colorModifier = this.inverted ? 1.0F - this.progress : this.progress;
        final int rgb = Color.HSBtoRGB(colorModifier * 0.33F, 1.0F, 1.0F);
        return new Color(rgb);
    }
}
