package me.mykindos.betterpvp.core.utilities.model;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ProgressBar {

    public static final char DEFAULT_SYMBOL = '\u258A';

    private String symbol = String.valueOf(DEFAULT_SYMBOL);
    private final float progress; // Scales from 0 to 1.0
    private final int barLength;
    private boolean inverted = false;
    private TextColor progressColor = NamedTextColor.GREEN;
    private TextColor remainingColor = NamedTextColor.RED;

    public ProgressBar(float progress, int barLength) {
        this.progress = progress;
        this.barLength = barLength;
    }

    /**
     * If inverted, the color will go from green to red. Otherwise, it will go from red to green.
     */
    public ProgressBar inverted() {
        this.inverted = true;
        return this;
    }

    public ProgressBar withProgressColor(TextColor color) {
        this.progressColor = color;
        return this;
    }

    public ProgressBar withRemainingColor(TextColor color) {
        this.remainingColor = color;
        return this;
    }

    public ProgressBar withCharacter(char character) {
        symbol = String.valueOf(character);
        return this;
    }

    public TextComponent build() {
        int progressCount = (int) Math.ceil(progress * barLength);
        if (inverted) {
            progressCount = barLength - progressCount;
        }
        int remainingCount = barLength - progressCount;

        TextComponent component = Component.empty();
        if (progressCount > 0) {
            component = component.append(Component.text(symbol.repeat(progressCount), progressColor));
        }
        if (remainingCount > 0) {
            component = component.append(Component.text(symbol.repeat(remainingCount), remainingColor));
        }

        return component;
    }

    public static ProgressBar withLength(float progress, int barLength) {
        Preconditions.checkArgument(progress >= 0 && progress <= 1.0, "Progress must be between 0 and 1.0");
        Preconditions.checkArgument(barLength > 0, "Bar length must be greater than 0");
        return new ProgressBar(progress, barLength);
    }

    public static ProgressBar withProgress(float progress) {
        return withLength(progress, 15);
    }

    public static ProgressBar empty(int barLength) {
        return new ProgressBar(0, barLength);
    }

}
