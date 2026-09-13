package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Everyone who must end up in the same place. Sites are located for a party rather than a player, so a group gets one
 * answer before anybody moves.
 * <p>
 * Members iterate in a fixed order, leader first. Anything that puts a party somewhere one at a time, such as an
 * arrival distribution, is reproducible because of it.
 */
@Value
public class Party {

    @NotNull UUID leader;

    /** Leader first, then everyone else in the order they were added. */
    @NotNull Set<UUID> members;

    private Party(@NotNull UUID leader, @NotNull Set<UUID> members) {
        this.leader = leader;
        // Not Set.copyOf: that returns a hash-ordered set, and its iteration order is randomised per JVM run, so the
        // leader-first ordering established below would survive construction on some restarts and not others.
        this.members = Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    /** A party of one, for a player travelling alone. */
    public static @NotNull Party solo(@NotNull UUID player) {
        return new Party(player, Set.of(player));
    }

    /** A party led by {@code leader}. The leader is always a member, and is first, whatever {@code members} says. */
    public static @NotNull Party of(@NotNull UUID leader, @NotNull Set<UUID> members) {
        final Set<UUID> roster = new LinkedHashSet<>();
        roster.add(leader);
        roster.addAll(members);
        return new Party(leader, roster);
    }

    public boolean has(@NotNull UUID player) {
        return members.contains(player);
    }

    public int size() {
        return members.size();
    }

    /** @return whether anybody in this party is already among {@code occupants} */
    public boolean overlaps(@NotNull Set<UUID> occupants) {
        return members.stream().anyMatch(occupants::contains);
    }
}
