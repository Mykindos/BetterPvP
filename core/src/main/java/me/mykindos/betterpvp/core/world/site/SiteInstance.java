package me.mykindos.betterpvp.core.world.site;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One live copy of a site: a Bukkit world plus whoever is standing in it. A permanent site has exactly one of these,
 * a pooled site several, and an owned site one per owner.
 */
@Getter
public class SiteInstance {

    public enum State {
        /** The world is being cloned or loaded. */
        PROVISIONING,
        /** The world is loaded and will take arrivals. */
        READY,
        /** The world is unloaded but its folder is intact, so it can be woken. */
        DORMANT,
        /** The world is being unloaded or deleted. */
        RELEASING
    }

    private final UUID id;
    private final SiteKey key;
    private final String worldName;
    private final long createdAt;
    private final Set<UUID> occupants = ConcurrentHashMap.newKeySet();

    @Setter
    private State state;

    @Setter
    private long lastVacatedAt;

    public SiteInstance(@NotNull UUID id, @NotNull SiteKey key, @NotNull String worldName, @NotNull State state) {
        this.id = id;
        this.key = key;
        this.worldName = worldName;
        this.createdAt = System.currentTimeMillis();
        this.lastVacatedAt = this.createdAt;
        this.state = state;
    }

    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    /** The first eight hex characters of the id, matching the short id used in a cloned world's name. */
    public @NotNull String getShortId() {
        return id.toString().substring(0, 8);
    }

    public void addOccupant(@NotNull UUID player) {
        occupants.add(player);
    }

    public void removeOccupant(@NotNull UUID player) {
        occupants.remove(player);
        lastVacatedAt = System.currentTimeMillis();
    }
}
