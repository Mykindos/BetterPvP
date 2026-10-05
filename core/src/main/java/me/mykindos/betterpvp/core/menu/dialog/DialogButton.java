package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Builder;
import lombok.Value;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A native dialog button. Its label can be glyph art: a label narrower than the button is drawn centred and is not
 * clipped. The button sprite itself is the global vanilla one.
 */
@Value
@Builder
public class DialogButton {

    Component label;
    @Nullable Component tooltip;
    @Builder.Default int width = 150;
    @Nullable DialogClick click;
}
