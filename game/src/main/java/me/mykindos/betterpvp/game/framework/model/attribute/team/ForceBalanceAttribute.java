package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Attribute for forcing a player from one team to move to another team if the game is unbalanced
 */
@GameScoped
public class ForceBalanceAttribute extends BoundAttribute<Boolean> {

    @Inject
    public ForceBalanceAttribute() {
        super("game.force-balance", true);
    }

    @Override
    public @Nullable Boolean parseValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    @Override
    public @NotNull Collection<Boolean> getPossibleValues() {
        return List.of(true, false);
    }
}
