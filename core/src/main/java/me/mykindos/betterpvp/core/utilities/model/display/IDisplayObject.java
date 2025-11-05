package me.mykindos.betterpvp.core.utilities.model.display;

import me.mykindos.betterpvp.core.client.gamer.Gamer;

import java.util.function.Function;

public interface IDisplayObject<T> {
    Function<Gamer, T> getProvider();
    void setInvalid(boolean invalid);
    boolean isInvalid();
}
