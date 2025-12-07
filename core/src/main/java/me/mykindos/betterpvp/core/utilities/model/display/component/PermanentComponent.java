package me.mykindos.betterpvp.core.utilities.model.display.component;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents a component that is shown on the action bar permanently.
 * This component will not expire.
 */
public final class PermanentComponent extends DisplayObject<Component> {

    /**
     * @param provider The component to show.
     */
    public PermanentComponent(@NotNull Function<Gamer, Component> provider) {
        super(provider);
    }
}
