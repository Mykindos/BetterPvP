package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * What a screen does after an action: re-render, open another screen, go back, close, show an error, or nothing.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionResult {

    public enum Kind { NONE, UPDATE, OPEN, BACK, CLOSE, ERROR }

    private final Kind kind;
    private final @Nullable Consumer<ScreenState> change;
    private final @Nullable String screen;
    private final Map<String, Object> state;
    private final @Nullable String errorKey;

    /** Keeps the screen as it is. Pressed art still settles. */
    public static ActionResult none() {
        return new ActionResult(Kind.NONE, null, null, Map.of(), null);
    }

    /** Re-renders with the current state. */
    public static ActionResult update() {
        return update(state -> { });
    }

    public static ActionResult update(Consumer<ScreenState> change) {
        return new ActionResult(Kind.UPDATE, change, null, Map.of(), null);
    }

    /** Opens a screen on top of this one. {@code screen} is {@code namespace:id}, or an id in this screen's namespace. */
    public static ActionResult open(String screen, Map<String, Object> state) {
        return new ActionResult(Kind.OPEN, null, screen, Map.copyOf(state), null);
    }

    public static ActionResult back() {
        return new ActionResult(Kind.BACK, null, null, Map.of(), null);
    }

    public static ActionResult close() {
        return new ActionResult(Kind.CLOSE, null, null, Map.of(), null);
    }

    /** Re-renders with the translated message under {@code error} in the state, and plays the error sound. */
    public static ActionResult error(String translationKey) {
        return new ActionResult(Kind.ERROR, null, null, Map.of(), translationKey);
    }
}
