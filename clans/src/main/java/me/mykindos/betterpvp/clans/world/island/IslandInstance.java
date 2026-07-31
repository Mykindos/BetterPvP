package me.mykindos.betterpvp.clans.world.island;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A live discovery island: a Bukkit world cloned from an {@link IslandTemplate}, tracked by
 * {@link IslandInstanceManager} for as long as it exists.
 */
@Getter
public class IslandInstance {

    private final UUID id;
    private final IslandTemplate template;
    private final String worldName;
    private final long createdAt;
    private final Set<UUID> occupants = ConcurrentHashMap.newKeySet();

    @Setter
    private IslandInstanceState state;

    @Setter
    private long lastVacatedAt;

    public IslandInstance(@NotNull UUID id, @NotNull IslandTemplate template, @NotNull String worldName) {
        this.id = id;
        this.template = template;
        this.worldName = worldName;
        this.createdAt = System.currentTimeMillis();
        this.lastVacatedAt = this.createdAt;
        this.state = IslandInstanceState.PROVISIONING;
    }

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    /** @return the first 8 hex characters of {@link #id}, matching the short id used in {@link #worldName}. */
    public @NotNull String getShortId() {
        return id.toString().substring(0, 8);
    }

    public void addOccupant(@NotNull UUID player) {
        occupants.add(player);
    }

    public void removeOccupant(@NotNull UUID player) {
        occupants.remove(player);
    }

}
