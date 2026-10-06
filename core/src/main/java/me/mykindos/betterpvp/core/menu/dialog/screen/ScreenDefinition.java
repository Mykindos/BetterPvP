package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A screen, parsed from a definition file or built in Java with {@link #builder()}. Both produce the same model.
 */
@Value
@Builder(toBuilder = true)
public class ScreenDefinition {

    /** Namespace and id, as in {@code core:dialog_test}. */
    String namespace;
    String id;
    int canvasWidth;
    int canvasHeight;
    @Nullable TextSpec name;
    /** Default state values. Keys a screen reads should be listed here. */
    @Singular("stateValue") Map<String, Object> state;
    /** Action names the screen expects to be bound when it opens, rather than registered globally. */
    @Singular Set<String> actions;
    @Singular Map<String, String> sounds;
    @Singular Map<String, Component> components;
    @Singular("backdropNode") List<Node> backdrop;
    @Singular List<Node> elements;
    @Singular List<Field> fields;
    @Singular List<NativeButton> buttons;
    @Builder.Default int columns = 2;
    @Nullable NativeButton exit;
    @Builder.Default boolean escapable = true;

    public String key() {
        return namespace + ":" + id;
    }

    /** A reusable element tree. Parameters are bound as state while its elements render. */
    @Value
    public static class Component {
        List<String> params;
        List<Node> elements;
    }

    /** An input below the canvas. Its value is the state key {@code bind}. */
    @Value
    @Builder
    public static class Field {
        public enum Kind { TEXT, TOGGLE, SLIDER, CHOICE }

        Kind kind;
        String bind;
        TextSpec label;
        @Builder.Default int width = 200;
        @Builder.Default int maxLength = 32;
        float start;
        float end;
        @Builder.Default float step = 1;
        @Singular List<Option> options;
    }

    @Value
    public static class Option {
        String id;
        TextSpec label;
    }

    /** A vanilla dialog button, in the grid below the canvas or as the footer exit. */
    @Value
    @Builder
    public static class NativeButton {
        TextSpec label;
        @Nullable TextSpec tooltip;
        @Builder.Default int width = 150;
        @Nullable ActionSpec onClick;
    }
}
