package me.mykindos.betterpvp.core.world.settler.wage;

import me.mykindos.betterpvp.core.world.settler.Settler;
import org.jetbrains.annotations.NotNull;

/** What a settler is paid. A server picks one model in config. */
public interface WageModel {

    /** Coins an hour for {@code settler}, working or not. 0 for a settler who is not paid. */
    double perHour(@NotNull Settler settler, boolean working);
}
