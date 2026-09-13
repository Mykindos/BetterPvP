package me.mykindos.betterpvp.core.world.site.crew;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.world.site.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A captain and whoever has come aboard with them, while they are still gathering.
 * <p>
 * A crew exists so that a group arrives <em>together</em>: at the same dock, and where the destination is instanced,
 * in the same copy of it. Travelling separately to the same place does neither.
 * <p>
 * The roster itself is a {@link Party}, which is what {@code locate} is given when the crew sails, so no conversion
 * happens at departure. What a crew adds around it is the assembly: where it is forming, how many it holds, who has
 * asked to come aboard, and whether it has left yet.
 */
@Getter
public class Crew {

    /** The captain and everyone aboard, captain first. Replaced rather than edited, since a party is immutable. */
    private Party party;

    /** Which hull this crew formed on — several crews can be forming on the same one. */
    private final String berthId;

    private final String worldName;
    private final int capacity;

    private final Set<UUID> requests = new LinkedHashSet<>();

    @Setter
    private CrewState state = CrewState.MUSTERING;

    public Crew(@NotNull UUID captain, @NotNull String berthId, @NotNull String worldName, int capacity) {
        this.party = Party.solo(captain);
        this.berthId = berthId;
        this.worldName = worldName;
        this.capacity = Math.max(1, capacity);
    }

    public @NotNull UUID getCaptain() {
        return party.getLeader();
    }

    /** Everyone aboard, the captain included. */
    public int size() {
        return party.size();
    }

    public boolean isFull() {
        return size() >= capacity;
    }

    public boolean isSailing() {
        return state == CrewState.SAILING;
    }

    public boolean has(@NotNull UUID player) {
        return party.has(player);
    }

    /** The captain first, then everyone else in the order they came aboard. */
    public @NotNull Set<UUID> roster() {
        return party.getMembers();
    }

    public @NotNull Set<UUID> pendingRequests() {
        return Collections.unmodifiableSet(requests);
    }

    public boolean hasRequestFrom(@NotNull UUID player) {
        return requests.contains(player);
    }

    /** Membership changes go through {@link CrewService}, which is where the rules for them live. */
    void aboard(@NotNull UUID player) {
        final Set<UUID> roster = new LinkedHashSet<>(party.getMembers());
        roster.add(player);
        party = Party.of(party.getLeader(), roster);
    }

    void ashore(@NotNull UUID player) {
        final Set<UUID> roster = new LinkedHashSet<>(party.getMembers());
        roster.remove(player);
        party = Party.of(party.getLeader(), roster);
    }

    /** Puts everyone but the captain off, which is what is left of a crew once it has been disbanded. */
    void emptied() {
        party = Party.solo(party.getLeader());
    }
}
