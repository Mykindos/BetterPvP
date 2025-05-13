package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for allowing more than the max limit for teams.
 */
@GameScoped
public class AllowOversizedTeamsAttribute extends BoundAttribute<Boolean> {

    @Inject
    public AllowOversizedTeamsAttribute() {
        super("game.allow-oversized-teams", true);
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
