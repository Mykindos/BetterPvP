package me.mykindos.betterpvp.core.utilities.model.actionbar;

import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

/**
 * Represents a single component that makes up an action bar.
 */
public sealed class DisplayComponent permits PermanentComponent, TimedComponent {

    private final Function<Gamer, Component> provider;
    private boolean isInvalid = false;

    protected DisplayComponent(Function<Gamer, Component> provider) {
        this.provider = provider;
    }

    protected void setInvalid(boolean invalid) {
        isInvalid = invalid;
    }

    Function<Gamer, Component> getProvider() {
        return provider;
    }

    protected boolean isInvalid() {
        return isInvalid;
    }
}
