package me.mykindos.betterpvp.core.menu.dialog.screen;

import lombok.Value;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Java code behind a named action. Bind it when opening a screen, or register it globally in {@link GuiRegistry}.
 */
@FunctionalInterface
public interface ActionHandler {

    ActionResult handle(Context context);

    /**
     * Who clicked, the screen's state (fields already written into it), and the action's evaluated arguments.
     */
    @Value
    class Context {
        Player player;
        ScreenState state;
        Map<String, Object> args;

        public Object arg(String name) {
            return args.get(name);
        }
    }
}
