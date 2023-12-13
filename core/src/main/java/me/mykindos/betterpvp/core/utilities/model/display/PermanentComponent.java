package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents a component that is shown on the action bar permanently.
 * This component will not expire.
 */
public final class PermanentComponent extends DisplayComponent {

    /**
     * @param provider The component to show.
     */
    public PermanentComponent(@Nullable Function<Gamer, Component> provider) {
        super(provider);
    }
}
