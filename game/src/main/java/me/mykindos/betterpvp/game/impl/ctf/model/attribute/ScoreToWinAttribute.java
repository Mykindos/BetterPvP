package me.mykindos.betterpvp.game.impl.ctf.model.attribute;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the score required to win in CTF.
 */
@GameScoped
public class ScoreToWinAttribute extends BoundAttribute<Integer> {

    @Inject
    public ScoreToWinAttribute() {
        super("game.ctf.score-to-win", 5);
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
        return "Score to win must be a positive integer.";
    }
}