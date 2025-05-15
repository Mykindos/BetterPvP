package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Attribute for immediately balancing.
 */
@GameScoped
public class AutoBalanceOnDeathAttribute extends BoundAttribute<Boolean> {

    @Inject
    public AutoBalanceOnDeathAttribute() {
        super("game.balance-on-death", false);
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
