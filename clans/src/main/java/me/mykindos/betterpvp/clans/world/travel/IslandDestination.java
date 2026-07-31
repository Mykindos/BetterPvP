package me.mykindos.betterpvp.clans.world.travel;

import me.mykindos.betterpvp.clans.world.Island;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Adapts an {@link Island} to a {@link Destination}. Delegates the actual arrival to a {@link LocalDestination}
 * resolving the island's world spawn point fresh on every check, so an island whose world has been unloaded simply
 * drops out of readiness rather than failing mid-voyage.
 */
public class IslandDestination implements Destination {

    private final Island island;
    private final LocalDestination local;

    public IslandDestination(@NotNull Island island) {
        this.island = island;
        final String slug = island.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        final Component displayName = Component.text(island.name());
        final ItemView icon = ItemView.builder()
                .material(Material.GRASS_BLOCK)
                .displayName(displayName)
                .build();
        this.local = new LocalDestination(Key.key("betterpvp", "island/" + slug), displayName, icon, this::resolveArrival);
    }

    public @NotNull Island island() {
        return island;
    }

    @Override
    public @NotNull Key key() {
        return local.key();
    }

    @Override
    public @NotNull Component displayName() {
        return local.displayName();
    }

    @Override
    public @NotNull ItemView icon() {
        return local.icon();
    }

    @Override
    public boolean isReady() {
        return local.isReady();
    }

    @Override
    public @NotNull CompletableFuture<Boolean> receive(@NotNull Player traveller) {
        return local.receive(traveller);
    }

    private @Nullable Location resolveArrival() {
        final var world = Bukkit.getWorld(island.worldName());
        return world == null ? null : world.getSpawnLocation();
    }
}
