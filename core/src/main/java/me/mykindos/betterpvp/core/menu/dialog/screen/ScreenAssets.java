package me.mykindos.betterpvp.core.menu.dialog.screen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

/**
 * The art the pack prepares for screens, and where each piece is placed. The pack generator
 * ({@code Resourcepack/tools/gui/build_gui.py}) implements the same rules, and both are checked against the golden
 * fixtures in {@code core/src/test/resources/gui-golden}.
 * <p>
 * Asset keys:
 * <ul>
 *   <li>{@code box:<style>:<w>x<h>} style art in the body</li>
 *   <li>{@code backdrop:<style>:<w>x<h>:<y>} style art in the title, its top at canvas y</li>
 *   <li>{@code pressed:<style>:<w>x<h>} the style's pressed art</li>
 *   <li>{@code hover:<style>:<w>x<h>:<canvas width>:<x>:<y>} hover art pinned at a canvas spot</li>
 *   <li>{@code sprite:<name>:<w>x<h>} and {@code anim:<name>:<w>x<h>:<frames>:<fps>} sprites</li>
 * </ul>
 * Art wider than {@link #MAX_GLYPH} splits into equal glyphs keyed {@code <key>#<part>}. Every glyph of a namespace,
 * sorted by key, takes a code from U+E000 in the font {@code betterpvp:gui/<namespace>}.
 */
public final class ScreenAssets {

    public static final int MAX_GLYPH = 256;
    /**
     * Backdrop art lines up with the canvas only while the body starts 63 px down, which on a 270 px screen (GUI scale 4
     * at 1080p) leaves this much canvas height.
     */
    public static final int BACKDROP_MAX_HEIGHT = 174;
    public static final int FIRST_CODE = 0xE000;
    public static final int LAST_CODE = 0xF8FF;

    private ScreenAssets() {
    }

    public static String box(String style, int width, int height) {
        return "box:" + style + ":" + width + "x" + height;
    }

    public static String backdrop(String style, int width, int height, int y) {
        return "backdrop:" + style + ":" + width + "x" + height + ":" + y;
    }

    public static String pressed(String style, int width, int height) {
        return "pressed:" + style + ":" + width + "x" + height;
    }

    public static String hover(String style, int width, int height, int canvasWidth, int x, int y) {
        return "hover:" + style + ":" + width + "x" + height + ":" + canvasWidth + ":" + x + ":" + y;
    }

    public static String sprite(String name, int width, int height, int frames, int fps) {
        return frames > 1
                ? "anim:" + name + ":" + width + "x" + height + ":" + frames + ":" + fps
                : "sprite:" + name + ":" + width + "x" + height;
    }

    /** Number of glyphs an asset splits into. Hover and animated art never split. */
    public static int parts(String key) {
        if (key.startsWith("hover:") || key.startsWith("anim:")) {
            return 1;
        }
        return (width(key) + MAX_GLYPH - 1) / MAX_GLYPH;
    }

    /** Width of glyph {@code part} of an asset. */
    public static int partWidth(String key, int part) {
        final int width = width(key);
        final int parts = parts(key);
        final int each = (width + parts - 1) / parts;
        return Math.min(each, width - part * each);
    }

    public static int width(String key) {
        return Integer.parseInt(size(key)[0]);
    }

    public static int height(String key) {
        return Integer.parseInt(size(key)[1]);
    }

    /** The glyph keys of a set of assets, sorted in code order. */
    public static List<String> glyphs(Set<String> assets) {
        final TreeSet<String> glyphs = new TreeSet<>();
        for (String asset : assets) {
            final int parts = parts(asset);
            if (parts == 1) {
                glyphs.add(asset);
            } else {
                for (int part = 0; part < parts; part++) {
                    glyphs.add(asset + "#" + part);
                }
            }
        }
        return new ArrayList<>(glyphs);
    }

    /**
     * Every asset a screen may need: all switch cases and when branches, every repeat slot up to its max, and the
     * declared assets of custom elements. {@code components} finds a component by name for {@code use} nodes.
     */
    public static Set<String> collect(ScreenDefinition screen,
                                      BiFunction<ScreenDefinition, String, ScreenDefinition.Component> components) {
        final Set<String> assets = new HashSet<>();
        final Collector collector = new Collector(screen, components, assets);
        collector.walk(screen.getBackdrop(), 0, 0, true, new ArrayList<>());
        collector.walk(screen.getElements(), 0, 0, false, new ArrayList<>());
        return assets;
    }

    /** The assets of some body nodes placed at a canvas origin, such as a custom element's declared art. */
    public static Set<String> collect(ScreenDefinition screen, List<Node> nodes, int originX, int originY,
                                      BiFunction<ScreenDefinition, String, ScreenDefinition.Component> components) {
        final Set<String> assets = new HashSet<>();
        new Collector(screen, components, assets).walk(nodes, originX, originY, false, new ArrayList<>());
        return assets;
    }

    /** Top-left of cell {@code index} of a layout, relative to the layout's parent origin. */
    public static int[] cell(Node.Group layout, int index) {
        final int column;
        final int row;
        switch (layout.getLayout()) {
            case ROW -> {
                column = index;
                row = 0;
            }
            case COLUMN -> {
                column = 0;
                row = index;
            }
            default -> {
                column = index % Math.max(1, layout.getColumns());
                row = index / Math.max(1, layout.getColumns());
            }
        }
        return new int[]{
                layout.getX() + column * (layout.getCellWidth() + layout.getGap()),
                layout.getY() + row * (layout.getCellHeight() + layout.getGap())};
    }

    private static String[] size(String key) {
        for (String segment : key.split(":")) {
            final int cross = segment.indexOf('x');
            if (cross > 0 && segment.chars().allMatch(c -> Character.isDigit(c) || c == 'x')) {
                return new String[]{segment.substring(0, cross), segment.substring(cross + 1)};
            }
        }
        throw new IllegalArgumentException("Asset key has no size: " + key);
    }

    private static final class Collector {
        private final ScreenDefinition screen;
        private final BiFunction<ScreenDefinition, String, ScreenDefinition.Component> components;
        private final Set<String> assets;

        private Collector(ScreenDefinition screen,
                          BiFunction<ScreenDefinition, String, ScreenDefinition.Component> components, Set<String> assets) {
            this.screen = screen;
            this.components = components;
            this.assets = assets;
        }

        void walk(List<Node> nodes, int originX, int originY, boolean backdrop, List<String> usePath) {
            for (Node node : nodes) {
                walk(node, originX, originY, backdrop, usePath);
            }
        }

        private void walk(Node node, int originX, int originY, boolean backdrop, List<String> usePath) {
            switch (node) {
                case Node.Text text -> {
                }
                case Node.Box box -> assets.add(backdrop
                        ? backdrop(box.getStyle(), box.getWidth(), box.getHeight(), originY + box.getY())
                        : box(box.getStyle(), box.getWidth(), box.getHeight()));
                case Node.Button button -> {
                    assets.add(box(button.getStyle(), button.getWidth(), button.getHeight()));
                    if (button.getSelectedStyle() != null) {
                        assets.add(box(button.getSelectedStyle(), button.getWidth(), button.getHeight()));
                    }
                    if (button.isPressed()) {
                        assets.add(pressed(button.getStyle(), button.getWidth(), button.getHeight()));
                        if (button.getSelectedStyle() != null) {
                            assets.add(pressed(button.getSelectedStyle(), button.getWidth(), button.getHeight()));
                        }
                    }
                    if (button.getHover() != null) {
                        assets.add(hover(button.getHover(), button.getWidth(), button.getHeight(), screen.getCanvasWidth(),
                                originX + button.getX(), originY + button.getY()));
                    }
                }
                case Node.Icon icon -> assets.add(sprite(icon.getSprite(), icon.getWidth(), icon.getHeight(),
                        icon.getFrames(), icon.getFps()));
                case Node.Group group -> {
                    for (int index = 0; index < group.getChildren().size(); index++) {
                        final int[] cell = cell(group, index);
                        walk(group.getChildren().get(index), originX + cell[0], originY + cell[1], backdrop, usePath);
                    }
                }
                case Node.Repeat repeat -> {
                    for (int index = 0; index < repeat.getMax(); index++) {
                        final int[] cell = cell(repeat.getLayout(), index);
                        walk(repeat.getChildren(), originX + cell[0], originY + cell[1], backdrop, usePath);
                    }
                }
                case Node.Switch choice -> {
                    choice.getCases().values().forEach(nodes -> walk(nodes, originX, originY, backdrop, usePath));
                    walk(choice.getFallback(), originX, originY, backdrop, usePath);
                }
                case Node.When when -> {
                    walk(when.getThen(), originX, originY, backdrop, usePath);
                    walk(when.getOtherwise(), originX, originY, backdrop, usePath);
                }
                case Node.Use use -> {
                    if (usePath.contains(use.getComponent())) {
                        throw new IllegalArgumentException("Component " + use.getComponent() + " uses itself through " + usePath);
                    }
                    final ScreenDefinition.Component component = components.apply(screen, use.getComponent());
                    if (component == null) {
                        throw new IllegalArgumentException("Unknown component " + use.getComponent());
                    }
                    final List<String> path = new ArrayList<>(usePath);
                    path.add(use.getComponent());
                    walk(component.getElements(), originX + use.getX(), originY + use.getY(), backdrop, path);
                }
                case Node.Custom custom -> walk(custom.getAssets(), originX + custom.getX(), originY + custom.getY(),
                        backdrop, usePath);
            }
        }
    }
}
