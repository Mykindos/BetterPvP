package me.mykindos.betterpvp.clans.world.island;

import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import me.mykindos.betterpvp.clans.world.crew.Crew;
import me.mykindos.betterpvp.clans.world.crew.CrewService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrewAllocationPolicyTest {

    private static final IslandTemplate TEMPLATE =
            new IslandTemplate("mining", Component.text("Mining Isle"), "templates/islands/mining", Material.IRON_PICKAXE, VoyageTiming.DEFAULT);

    private CrewService crewService;
    private CrewAllocationPolicy policy;
    private IslandInstance instance;

    private UUID captainId;
    private UUID memberId;
    private UUID strangerId;

    @BeforeEach
    void setUp() {
        crewService = new CrewService();
        policy = new CrewAllocationPolicy(crewService);
        instance = new IslandInstance(UUID.randomUUID(), TEMPLATE, "islands/mining/a1b2c3d4");

        captainId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
    }

    private static Player playerWith(UUID uuid) {
        final Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    private Crew crewOfTwo() {
        final Crew crew = crewService.muster(captainId, "north_berth", "Season-2/Spawn", 6);
        crewService.join(memberId, crew);
        return crew;
    }

    /** The whole point: the captain allocates, and the rest of the crew follow into that same instance. */
    @Test
    @DisplayName("a crewmate may follow their captain into an instance")
    void crewmateFollowsCaptain() {
        crewOfTwo();
        instance.addOccupant(captainId);

        assertTrue(policy.canAccept(instance, playerWith(memberId)));
    }

    @Test
    @DisplayName("the captain may follow a crewmate who arrived first")
    void captainFollowsCrewmate() {
        crewOfTwo();
        instance.addOccupant(memberId);

        assertTrue(policy.canAccept(instance, playerWith(captainId)));
    }

    @Test
    @DisplayName("somebody with no crew never shares, so solo travel is unchanged")
    void solitaryTravellerGetsTheirOwnIsland() {
        instance.addOccupant(captainId);

        assertFalse(policy.canAccept(instance, playerWith(strangerId)));
    }

    @Test
    @DisplayName("a crewed player does not join an island held by people they are not sailing with")
    void otherCrewsAreNotShared() {
        crewOfTwo();
        instance.addOccupant(strangerId);

        assertFalse(policy.canAccept(instance, playerWith(memberId)),
                "sharing is by crew, not by whoever happens to be there");
    }

    @Test
    @DisplayName("an empty instance is not shared into — the first arrival allocates it")
    void emptyInstanceIsNotShared() {
        crewOfTwo();

        assertFalse(policy.canAccept(instance, playerWith(memberId)));
    }

    @Test
    @DisplayName("a crew that disbanded mid-flight no longer shares")
    void disbandedCrewStopsSharing() {
        final Crew crew = crewOfTwo();
        instance.addOccupant(captainId);
        crewService.disband(crew);

        assertFalse(policy.canAccept(instance, playerWith(memberId)));
    }

    @Test
    @DisplayName("a full instance refuses even a crewmate")
    void capacityIsARunawayGuard() {
        final Crew crew = crewOfTwo();
        instance.addOccupant(captainId);
        for (int extra = 0; extra < policy.capacity(); extra++) {
            final UUID filler = UUID.randomUUID();
            crewService.join(filler, crew);
            instance.addOccupant(filler);
        }

        assertFalse(policy.canAccept(instance, playerWith(memberId)));
    }
}
