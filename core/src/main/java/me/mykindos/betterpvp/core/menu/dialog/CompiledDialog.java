package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Value;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A {@link DialogScreen} turned into what the client receives: components, widths and click keys.
 */
@Value
public class CompiledDialog {

    Component name;
    Component title;
    Component body;
    int bodyWidth;
    List<DialogField> fields;
    List<Button> buttons;
    int columns;
    @Nullable Button exit;
    boolean escapable;

    @Value
    public static class Button {
        Component label;
        @Nullable Component tooltip;
        int width;
        @Nullable Key action;
    }
}
