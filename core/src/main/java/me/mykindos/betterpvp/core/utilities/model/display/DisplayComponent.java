package me.mykindos.betterpvp.core.utilities.model.display;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.gamer.Gamer;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

/**
 * Represents a single component that makes up an action bar.
 */
public abstract class DisplayComponent {

    private final Function<Gamer, Component> provider;
    private boolean isInvalid = false;

    protected DisplayComponent(Function<Gamer, Component> provider) {
        Preconditions.checkNotNull(provider, "provider");
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
