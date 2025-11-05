package me.mykindos.betterpvp.core.utilities.model.display;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;


@Getter
public class DisplayObject<T> implements IDisplayObject<T> {
    private final Function<Gamer, T> provider;
    @Setter
    private boolean invalid = false;

    public DisplayObject(@NotNull Function<Gamer, T> provider) {
        Preconditions.checkNotNull(provider, "provider cannot be null");
        this.provider = provider;
    }
}
