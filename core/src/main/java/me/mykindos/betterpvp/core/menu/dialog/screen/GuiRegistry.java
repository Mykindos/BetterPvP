package me.mykindos.betterpvp.core.menu.dialog.screen;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.utilities.Resources;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Extensions every screen can use, registered by any plugin under its namespace: actions, components, element types,
 * text styles, sounds and formatters. Screens name them as {@code namespace:name}, or by bare name, which looks in the
 * screen's own namespace first and then in {@code core}.
 */
@Singleton
public class GuiRegistry {

    private final Map<String, ActionHandler> actions = new HashMap<>();
    private final Map<String, ScreenDefinition.Component> components = new HashMap<>();
    private final Map<String, ElementType> elementTypes = new HashMap<>();
    private final Map<String, UnaryOperator<Component>> textStyles = new HashMap<>();
    private final Map<String, String> sounds = new HashMap<>();
    private final Map<String, BiFunction<Object, List<Object>, Object>> formatters = new HashMap<>();

    public GuiRegistry() {
        textStyle("core", "body", text -> text.color(TextColor.color(0x2A2C30)).shadowColor(ShadowColor.none()));
        textStyle("core", "muted", text -> text.color(TextColor.color(0x4A4C51)).shadowColor(ShadowColor.none()));
        textStyle("core", "value", text -> text.color(TextColor.color(0x2A2C30)).shadowColor(ShadowColor.none()));
        textStyle("core", "title", text -> text.color(TextColor.color(0x2A2C30)).shadowColor(ShadowColor.none()));
        textStyle("core", "error", text -> text.color(TextColor.color(0xA61B1B)).shadowColor(ShadowColor.none()));
        textStyle("core", "on_primary", text -> text.color(TextColor.color(0xFFFFFF)).shadowColor(ShadowColor.none()));

        sound("core", "click", "minecraft:ui.button.click");
        sound("core", "open", "minecraft:item.book.page_turn");
        sound("core", "close", "minecraft:item.book.put");
        sound("core", "error", "minecraft:block.note_block.bass");

        formatters.put("upper", (value, args) -> Expressions.text(value).toUpperCase(Locale.ROOT));
        formatters.put("lower", (value, args) -> Expressions.text(value).toLowerCase(Locale.ROOT));
        formatters.put("default", (value, args) -> Expressions.truthy(value) || args.isEmpty() ? value : args.getFirst());
        formatters.put("short", (value, args) -> shortNumber(value));
    }

    public void action(String namespace, String name, ActionHandler handler) {
        actions.put(namespace + ":" + name, handler);
    }

    public void component(String namespace, String name, ScreenDefinition.Component component) {
        components.put(namespace + ":" + name, component);
    }

    public void elementType(String namespace, String name, ElementType type) {
        elementTypes.put(namespace + ":" + name, type);
    }

    /** A named text look: colour, shadow, decorations. The theme font is applied separately. */
    public void textStyle(String namespace, String name, UnaryOperator<Component> style) {
        textStyles.put(namespace + ":" + name, text -> style.apply(text.font() == null ? text.font(Resources.Font.UI) : text));
    }

    /** The sound for a screen event ({@code open}, {@code close}, {@code click}, {@code error}) when a screen sets none. */
    public void sound(String namespace, String event, String sound) {
        sounds.put(namespace + ":" + event, sound);
    }

    /** A formatter usable in bindings as {@code {value | name:arg}}. Formatters are global, not namespaced. */
    public void formatter(String name, BiFunction<Object, List<Object>, Object> formatter) {
        formatters.put(name, formatter);
    }

    @Nullable
    ActionHandler action(String namespace, String name) {
        return find(actions, namespace, name);
    }

    @Nullable
    ScreenDefinition.Component component(String namespace, String name) {
        return find(components, namespace, name);
    }

    @Nullable
    ElementType elementType(String namespace, String name) {
        return find(elementTypes, namespace, name);
    }

    @Nullable
    UnaryOperator<Component> textStyle(String namespace, String name) {
        return find(textStyles, namespace, name);
    }

    @Nullable
    String sound(String namespace, String event) {
        return find(sounds, namespace, event);
    }

    Map<String, BiFunction<Object, List<Object>, Object>> formatters() {
        return formatters;
    }

    private static <T> T find(Map<String, T> map, String namespace, String name) {
        if (name.contains(":")) {
            return map.get(name);
        }
        final T own = map.get(namespace + ":" + name);
        return own != null ? own : map.get("core:" + name);
    }

    private static Object shortNumber(Object value) {
        if (!(value instanceof Number number)) {
            return value;
        }
        final double amount = number.doubleValue();
        if (Math.abs(amount) >= 1_000_000) {
            return trim(amount / 1_000_000) + "m";
        }
        if (Math.abs(amount) >= 1_000) {
            return trim(amount / 1_000) + "k";
        }
        return Expressions.text(amount);
    }

    private static String trim(double value) {
        final String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
