package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Lets a crew share one island: an instance already holding somebody you are sailing with will take you too.
 * <p>
 * Replaces the solo policy, which refused every share and so gave a crew of four four separate islands — arriving
 * apart, which is the one thing crewing up is meant to prevent. Somebody travelling alone is unaffected: they are in no
 * crew, nobody aboard is a crewmate of theirs, and they get a fresh instance exactly as before.
 */
@Singleton
public class CrewAllocationPolicy implements InstanceAllocationPolicy {

    /**
     * An upper bound on how many an instance will take. Larger than any hull's capacity, since this only has to stop a
     * runaway — the real limit is how many the ship held when it sailed.
     */
    private static final int MAX_OCCUPANTS = 16;

    private final CrewService crewService;

    @Inject
    public CrewAllocationPolicy(@NotNull CrewService crewService) {
        this.crewService = crewService;
    }

    @Override
    public boolean canAccept(@NotNull IslandInstance instance, @NotNull Player player) {
        if (instance.getOccupants().size() >= MAX_OCCUPANTS) {
            return false;
        }

        // Sharing is by crew, not by proximity or luck: only somebody already sailing with an occupant may follow them
        // in, so an unrelated traveller never lands in a crew's island.
        return crewService.crewOf(player.getUniqueId())
                .map(crew -> instance.getOccupants().stream().anyMatch(crew::has))
                .orElse(false);
    }

    @Override
    public int capacity() {
        return MAX_OCCUPANTS;
    }
}
