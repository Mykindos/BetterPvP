package me.mykindos.betterpvp.core.utilities.model.display;

import java.util.function.Function;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a component that is shown on the action bar permanently.
 * This component will not expire.
 */
public final class PermanentComponent extends GamerDisplayObject<Component> {

    /**
     * @param provider The component to show.
     */
    public PermanentComponent(@NotNull Function<Gamer, Component> provider) {
        super(provider);
    }
}
