package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.With;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * An input on a dialog screen. Values come back with native button clicks, and re-rendering a screen sends the last
 * values back as each field's initial value.
 */
public sealed interface DialogField permits DialogField.Text, DialogField.Toggle, DialogField.Slider, DialogField.Choice {

    String getKey();

    /** This field with its initial value replaced by a value a click carried. */
    DialogField withValue(Object value);

    @Value
    @Builder
    @With
    final class Text implements DialogField {
        String key;
        Component label;
        @Builder.Default int width = 200;
        @Builder.Default int maxLength = 32;
        @Builder.Default String initial = "";

        @Override
        public DialogField withValue(Object value) {
            return withInitial((String) value);
        }
    }

    @Value
    @Builder
    @With
    final class Toggle implements DialogField {
        String key;
        Component label;
        boolean initial;

        @Override
        public DialogField withValue(Object value) {
            return withInitial((Boolean) value);
        }
    }

    @Value
    @Builder
    @With
    final class Slider implements DialogField {
        String key;
        Component label;
        @Builder.Default int width = 200;
        float start;
        float end;
        @Builder.Default float step = 1;
        float initial;

        @Override
        public DialogField withValue(Object value) {
            return withInitial((Float) value);
        }
    }

    @Value
    @Builder
    @With
    final class Choice implements DialogField {
        String key;
        Component label;
        @Builder.Default int width = 200;
        @Singular List<Option> options;
        String initial;

        @Override
        public DialogField withValue(Object value) {
            return withInitial((String) value);
        }
    }

    @Value
    class Option {
        String id;
        Component display;
    }
}
