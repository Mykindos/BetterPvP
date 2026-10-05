package me.mykindos.betterpvp.core.menu.dialog.screen;

import java.util.List;

/**
 * An element drawn by Java, used from a screen as {@code {"custom": "<namespace>:<name>"}}. It draws through the
 * {@link RenderContext}, which only hands out art the element declared in its {@code assets}.
 */
public interface ElementType {

    /** Problems with a custom node's properties, checked when screens load. Empty when it is valid. */
    default List<String> validate(Node.Custom node) {
        return List.of();
    }

    void render(Node.Custom node, RenderContext context);
}
