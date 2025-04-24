package me.mykindos.betterpvp.game.framework.model.attribute.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttribute;
import me.mykindos.betterpvp.game.framework.model.attribute.GlobalAttributeModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Attribute for configuring whether the game starts in a paused state.
 */
@Singleton
public class StartPausedAttribute extends GameAttribute<Boolean> {

    @Inject
    public StartPausedAttribute() {
        super("game.start-paused", false);
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
