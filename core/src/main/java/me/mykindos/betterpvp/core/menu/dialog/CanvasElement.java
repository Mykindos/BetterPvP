package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * One thing placed on a {@link DialogCanvas}: its top-left corner and size in GUI pixels, what it draws, and
 * optionally a tooltip, a click, hover art and pressed content.
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
    private @Nullable Component hover;
    private @Nullable Component pressed;

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

    /**
     * Art drawn in place of this element while the mouse is over it. The art travels in the hover tooltip and the
     * pack's text shader moves it from the mouse onto the element, so it must be a hover glyph generated for this
     * element's position. It takes the place of a text tooltip.
     */
    public CanvasElement hover(Component art) {
        this.hover = art;
        return this;
    }

    /** Content shown in place of this element for a moment after it is clicked, before the click runs. */
    public CanvasElement pressed(Component content) {
        this.pressed = content;
        return this;
    }
}
