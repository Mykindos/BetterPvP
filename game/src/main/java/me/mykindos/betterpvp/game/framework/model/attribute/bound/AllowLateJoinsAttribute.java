package me.mykindos.betterpvp.game.framework.model.attribute.bound;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for allowing players to respawn.
 */
@GameScoped
public class AllowLateJoinsAttribute extends BoundAttribute<Boolean> {

    @Inject
    public AllowLateJoinsAttribute() {
        super("game.allow-late-joins", true);
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
