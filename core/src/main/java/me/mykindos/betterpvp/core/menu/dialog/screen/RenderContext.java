package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Getter;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.dialog.CanvasElement;
import me.mykindos.betterpvp.core.menu.dialog.DialogCanvas;
import me.mykindos.betterpvp.core.menu.dialog.DialogClick;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * What a render can see at one point of the element tree: the player, the bindings in scope, the canvas origin, the
 * namespace's art, and how to turn an action into a click. Custom elements draw through it.
 */
public class RenderContext {

    @Getter
    private final Player player;
    @Getter
    private final ScreenDefinition screen;
    @Getter
    private final DialogCanvas canvas;
    @Getter
    private final int originX;
    @Getter
    private final int originY;
    private final Function<String, Object> lookup;
    private final GuiRegistry registry;
    private final AssetTable assets;
    private final BiFunction<ActionSpec, Function<String, Object>, DialogClick> clicks;
    private final @Nullable Set<String> allowed;

    RenderContext(Player player, ScreenDefinition screen, DialogCanvas canvas, int originX, int originY,
                  Function<String, Object> lookup, GuiRegistry registry, AssetTable assets,
                  BiFunction<ActionSpec, Function<String, Object>, DialogClick> clicks, @Nullable Set<String> allowed) {
        this.player = player;
        this.screen = screen;
        this.canvas = canvas;
        this.originX = originX;
        this.originY = originY;
        this.lookup = lookup;
        this.registry = registry;
        this.assets = assets;
        this.clicks = clicks;
        this.allowed = allowed;
    }

    /** This context moved by an offset. */
    RenderContext offset(int x, int y) {
        return new RenderContext(player, screen, canvas, originX + x, originY + y, lookup, registry, assets, clicks, allowed);
    }

    /** This context with extra bindings in front of the current ones. */
    RenderContext bind(Map<String, Object> values) {
        final Map<String, Object> local = new HashMap<>(values);
        return new RenderContext(player, screen, canvas, originX, originY,
                name -> local.containsKey(name) ? local.get(name) : lookup.apply(name), registry, assets, clicks, allowed);
    }

    /** This context limited to the art a custom element declared. */
    RenderContext restrict(Set<String> declared) {
        return new RenderContext(player, screen, canvas, originX, originY, lookup, registry, assets, clicks, declared);
    }

    public Object evaluate(String template) {
        return Expressions.evaluate(template, lookup, registry.formatters());
    }

    public boolean test(String condition) {
        return Expressions.truthy(evaluate(condition));
    }

    /** Text resolved for the player: a template, or a translation with template arguments. */
    public Component text(TextSpec spec) {
        if (spec.getKey() == null) {
            final Object value = evaluate(spec.getTemplate());
            // A binding to a component, such as a translated error message, draws as that component.
            return value instanceof Component component ? component : Component.text(Expressions.text(value));
        }
        final List<Component> args = spec.getArgs().stream()
                .map(arg -> (Component) Component.text(Expressions.text(evaluate(arg))))
                .toList();
        return Translations.render(Translations.component(spec.getKey(), args.toArray(Component[]::new)), player.locale());
    }

    /** Text in a named text style. */
    public Component styled(TextSpec spec, String style) {
        final UnaryOperator<Component> look = registry.textStyle(screen.getNamespace(), style);
        if (look == null) {
            throw new IllegalArgumentException("Unknown text style " + style);
        }
        return look.apply(text(spec));
    }

    @Nullable
    public DialogClick click(@Nullable ActionSpec action) {
        return action == null ? null : clicks.apply(action, lookup);
    }

    /** Places text at a canvas spot relative to this context, aligned in a box {@code width} wide. */
    public CanvasElement text(int x, int y, Component text, Node.Align align, int width) {
        final int drawn = UtilFont.componentWidth(text) - 1;
        final int left = Math.max(0, originX + switch (align) {
            case LEFT -> x;
            case CENTER -> x + (width - drawn) / 2;
            case RIGHT -> x + width - drawn;
        });
        // Text past the canvas edge, such as a long translation, is cut to fit rather than failing the whole screen.
        final int room = canvas.getWidth() - left - 1;
        final Component shown = drawn > room ? TextWrap.lines(text, room, 1).getFirst() : text;
        return canvas.text(left, originY + y, shown);
    }

    /**
     * Places an asset's art at a canvas spot relative to this context, every part side by side. Returns the elements
     * placed, so callers can attach clicks and hover art.
     */
    public List<CanvasElement> art(String asset, int x, int y) {
        check(asset);
        final List<CanvasElement> placed = new ArrayList<>();
        int left = originX + x;
        for (int part = 0; part < ScreenAssets.parts(asset); part++) {
            final int width = ScreenAssets.partWidth(asset, part);
            // A glyph advances one pixel past its image.
            placed.add(canvas.place(left, originY + y, assets.glyph(asset, part), width + 1, ScreenAssets.height(asset)));
            left += width;
        }
        return placed;
    }

    /** The single glyph of an asset that never splits, such as pressed or hover art. */
    public Component glyph(String asset) {
        return glyph(asset, 0);
    }

    /** One glyph of an asset, such as a frame of a frames: sprite. */
    public Component glyph(String asset, int part) {
        check(asset);
        return assets.glyph(asset, part);
    }

    private void check(String asset) {
        if (allowed != null && !allowed.contains(asset)) {
            throw new IllegalArgumentException("Custom element asked for " + asset + ", which its assets do not declare");
        }
    }

    BiFunction<ScreenDefinition, String, ScreenDefinition.Component> components() {
        return (definition, name) -> {
            final ScreenDefinition.Component own = definition.getComponents().get(name);
            return own != null ? own : registry.component(definition.getNamespace(), name);
        };
    }

    GuiRegistry registry() {
        return registry;
    }
}
