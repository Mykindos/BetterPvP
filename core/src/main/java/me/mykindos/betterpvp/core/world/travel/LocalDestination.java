package me.mykindos.betterpvp.core.world.travel;

import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A {@link Destination} that resolves to a {@link Location} on this server — a fixed landmark, a clan core, or the
 * spawn point of some world. Arrival location is resolved lazily through {@code arrival} rather than captured once,
 * so a destination whose world is not currently loaded reports {@link #isReady()} as {@code false} instead of
 * teleporting into a stale location.
 */
public class LocalDestination implements Destination {

    private final Key key;
    private final Component displayName;
    private final ItemView icon;
    private final Supplier<Location> arrival;

    public LocalDestination(@NotNull Key key, @NotNull Component displayName, @NotNull ItemView icon, @NotNull Supplier<Location> arrival) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.arrival = arrival;
    }

    @Override
    public @NotNull Key key() {
        return key;
    }

    @Override
    public @NotNull Component displayName() {
        return displayName;
    }

    @Override
    public @NotNull ItemView icon() {
        return icon;
    }

    @Override
    public boolean isReady() {
        return resolveArrival() != null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller) {
        final Location location = resolveArrival();
        if (location == null) {
            return CompletableFuture.completedFuture(false);
        }

        return traveller.teleportAsync(location.clone().add(0, 0.01, 0));
    }

    private @Nullable Location resolveArrival() {
        return arrival.get();
    }
}
