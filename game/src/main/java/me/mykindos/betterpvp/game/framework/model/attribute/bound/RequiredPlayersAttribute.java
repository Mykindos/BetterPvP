package me.mykindos.betterpvp.game.framework.model.attribute.bound;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.listener.state.TransitionHandler;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the required number of players.
 */
@GameScoped
public class RequiredPlayersAttribute extends BoundAttribute<Integer> {

    @Inject
    public RequiredPlayersAttribute() {
        super("game.required-players", 2);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be positive
        return value > 0;
    }

    @Override
    @Nullable
    public Integer parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Required players must be a positive integer.";
    }
}
