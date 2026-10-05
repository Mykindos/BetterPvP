package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * One thing placed on a {@link DialogCanvas}: its top-left corner and size in GUI pixels, what it draws, and
 * optionally a tooltip and a click.
 */
@Getter
public class CanvasElement {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final Component content;
    private @Nullable Component tooltip;
    private @Nullable DialogClick click;

    CanvasElement(int x, int y, int width, int height, Component content) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.content = content;
    }

    public CanvasElement tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public CanvasElement onClick(DialogClick click) {
        this.click = click;
        return this;
    }
}
