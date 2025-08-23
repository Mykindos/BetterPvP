package me.mykindos.betterpvp.core.utilities.model.display;

import java.util.function.Function;
import me.mykindos.betterpvp.core.client.gamer.Gamer;

public interface IGamerDisplayObject<T> {
    Function<Gamer, T> getProvider();
    void setInvalid(boolean invalid);
    boolean isInvalid();
}
