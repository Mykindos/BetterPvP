package me.mykindos.betterpvp.clans.world.crew;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A captain and whoever has come aboard with them.
 * <p>
 * A crew exists so that a group arrives <em>together</em>: at the same dock, and — where the destination is instanced —
 * in the same copy of it. Travelling separately to the same place does neither.
 * <p>
 * Membership order is kept because the crew menu shows it, and a roster that reshuffles between openings is hard to
 * click accurately.
 */
@Getter
public class Crew {

    private final UUID captain;

    /** Which hull this crew formed on — several crews can be forming on the same one. */
    private final String berthId;

    private final String worldName;
    private final int capacity;

    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> requests = new LinkedHashSet<>();

    @Setter
    private CrewState state = CrewState.MUSTERING;

    public Crew(@NotNull UUID captain, @NotNull String berthId, @NotNull String worldName, int capacity) {
        this.captain = captain;
        this.berthId = berthId;
        this.worldName = worldName;
        this.capacity = Math.max(1, capacity);
    }

    /** Everyone aboard, the captain included. */
    public int size() {
        return members.size() + 1;
    }

    public boolean isFull() {
        return size() >= capacity;
    }

    public boolean isSailing() {
        return state == CrewState.SAILING;
    }

    public boolean has(@NotNull UUID player) {
        return captain.equals(player) || members.contains(player);
    }

    /** The captain first, then everyone else in the order they came aboard. */
    public @NotNull Set<UUID> roster() {
        final Set<UUID> roster = new LinkedHashSet<>();
        roster.add(captain);
        roster.addAll(members);
        return Collections.unmodifiableSet(roster);
    }

    public @NotNull Set<UUID> pendingRequests() {
        return Collections.unmodifiableSet(requests);
    }

    public boolean hasRequestFrom(@NotNull UUID player) {
        return requests.contains(player);
    }
}
