package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * When balancing teams mid game, whether or not to keep players on their initial team
 */
@GameScoped
public class KeepSameTeamAttribute extends BoundAttribute<Boolean> {
    @Inject
    public KeepSameTeamAttribute() {
        super("game.team.keep-same-team", true);
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
