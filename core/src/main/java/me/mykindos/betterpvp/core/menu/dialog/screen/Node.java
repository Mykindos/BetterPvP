package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One element of a screen's canvas or backdrop. Positions are GUI pixels from the top-left of the canvas, or from the
 * cell a layout helper gives the node. Positions are always numbers, so the pack can prepare art for every spot.
 */
public sealed interface Node permits Node.Text, Node.Box, Node.Button, Node.Icon, Node.Group, Node.Repeat,
        Node.Switch, Node.When, Node.Use, Node.Custom {

    enum Align { LEFT, CENTER, RIGHT }

    enum Layout { ROW, COLUMN, GRID }

    /** Text. {@code width} is the box {@code align} works in, so 0 with LEFT draws from x. */
    @Value
    @Builder
    final class Text implements Node {
        int x;
        int y;
        TextSpec text;
        @Builder.Default String style = "body";
        @Builder.Default Align align = Align.LEFT;
        int width;
        @Nullable TextSpec tooltip;
        @Nullable ActionSpec onClick;
    }

    /** Style art at a size: a panel, a frame, a divider. In the backdrop it is drawn in the title. */
    @Value
    @Builder
    final class Box implements Node {
        int x;
        int y;
        int width;
        int height;
        String style;
    }

    /**
     * A clickable box with a centred label. {@code selected} is a binding that swaps the art to {@code selectedStyle}.
     * {@code hover} names the hover style the pack draws over it, {@code pressed} shows the style's pressed art for a
     * moment after a click.
     */
    @Value
    @Builder
    final class Button implements Node {
        int x;
        int y;
        int width;
        int height;
        String style;
        @Nullable String selectedStyle;
        @Nullable String selected;
        @Nullable TextSpec label;
        @Builder.Default String labelStyle = "body";
        @Nullable String hover;
        boolean pressed;
        @Nullable TextSpec tooltip;
        @Nullable ActionSpec onClick;
    }

    /** A sprite from the pack's sprite sources, optionally looping through frames on the client. */
    @Value
    @Builder
    final class Icon implements Node {
        int x;
        int y;
        int width;
        int height;
        String sprite;
        int frames;
        int fps;
        @Nullable TextSpec tooltip;
        @Nullable ActionSpec onClick;
    }

    /**
     * Places children in cells. ROW and COLUMN step by the cell size plus the gap. GRID fills {@code columns} cells per
     * row. A child's own x and y are offsets inside its cell.
     */
    @Value
    @Builder
    final class Group implements Node {
        Layout layout;
        int x;
        int y;
        int cellWidth;
        int cellHeight;
        int gap;
        @Builder.Default int columns = 1;
        @Singular List<Node> children;
    }

    /**
     * Repeats its children once per entry of a state list, up to {@code max}, each copy in the next cell of the
     * layout. The entry is bound as {@code as} (default {@code item}) and its index as {@code index}.
     */
    @Value
    @Builder
    final class Repeat implements Node {
        String list;
        @Builder.Default String as = "item";
        int max;
        Group layout;
        @Singular List<Node> children;
    }

    /** Shows the case whose name matches the binding, or the fallback. */
    @Value
    @Builder
    final class Switch implements Node {
        String value;
        @Builder.Default Map<String, List<Node>> cases = Map.of();
        @Builder.Default List<Node> fallback = List.of();
    }

    @Value
    @Builder
    final class When implements Node {
        String condition;
        @Builder.Default List<Node> then = List.of();
        @Builder.Default List<Node> otherwise = List.of();
    }

    /** Places a component's elements, offset by x and y, with its parameters bound. */
    @Value
    @Builder
    final class Use implements Node {
        String component;
        int x;
        int y;
        @Builder.Default Map<String, String> with = Map.of();
    }

    /**
     * An element drawn by a registered {@link ElementType}. {@code assets} declares every piece of art the type may
     * use, written as nodes, so the pack prepares them.
     */
    @Value
    @Builder
    final class Custom implements Node {
        String type;
        int x;
        int y;
        int width;
        int height;
        @Singular Map<String, Object> properties;
        @Singular List<Node> assets;
    }
}
