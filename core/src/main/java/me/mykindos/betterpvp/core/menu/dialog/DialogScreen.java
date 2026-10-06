package me.mykindos.betterpvp.core.menu.dialog;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Everything one dialog screen shows. The backdrop draws in the dialog title, which is never clipped. The canvas is
 * the interactive body. Fields and grid buttons follow the canvas, and the exit button sits in the footer.
 * <p>
 * Both canvases share one origin, the top-left of the body, so backdrop art and canvas regions line up. That holds
 * while the canvas fits between the 63 px header and the 33 px footer.
 */
@Value
@Builder
public class DialogScreen {

    /** Name of the screen where the client lists it, never drawn on the screen itself. */
    Component name;
    @Builder.Default DialogCanvas backdrop = new DialogCanvas(0);
    DialogCanvas canvas;
    @Singular List<DialogField> fields;
    @Singular List<DialogButton> buttons;
    @Builder.Default int columns = 2;
    @Nullable DialogButton exit;
    @Builder.Default boolean escapable = true;
}
