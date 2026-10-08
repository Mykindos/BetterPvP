package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.core.world.construction.Job;
import me.mykindos.betterpvp.core.world.construction.JobKind;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePlacedEvent;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureStatus;
import me.mykindos.betterpvp.core.world.construction.StructureStatusChangeEvent;
import me.mykindos.betterpvp.core.world.settler.crew.CrewRule;
import me.mykindos.betterpvp.core.world.site.SiteInstance;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrewNoticesTest {

    private static final SiteKey SITE = Camps.keyFor(42);
    private static final String WORLD = "camp-42";

    private final Camps camps = mock(Camps.class);
    private final SiteInstances instances = mock(SiteInstances.class);
    private final StructureCatalogue catalogue = mock(StructureCatalogue.class);
    private final CrewRule rule = mock(CrewRule.class);
    private final World world = mock(World.class);
    private final Player member = mock(Player.class);
    private final Player visitor = mock(Player.class);
    private MockedStatic<Bukkit> bukkit;
    private CrewNotices notices;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getWorld(WORLD)).thenReturn(world);
        when(instances.forKey(any())).thenReturn(List.of(
                new SiteInstance(UUID.randomUUID(), SITE, WORLD, SiteInstance.State.READY)));
        when(world.getPlayers()).thenReturn(List.of(member, visitor));
        when(camps.isMember(member, world)).thenReturn(true);
        when(catalogue.find(any())).thenReturn(Optional.empty());
        when(rule.threshold(any(), any())).thenReturn(4);
        notices = new CrewNotices(camps, instances, catalogue, rule);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private static PlacedStructure structure(String hold) {
        final PlacedStructure structure = new PlacedStructure(UUID.randomUUID(), CampStructures.WORKSHOP,
                new StructurePosition(0, 64, 0, 0), StructureCondition.UNDER_CONSTRUCTION);
        final Job job = Job.start(JobKind.BUILD, Duration.ofMinutes(10), ResourceCost.NONE, 0, 0);
        if (hold != null) {
            job.hold(hold, 0);
        }
        structure.setJob(job);
        return structure;
    }

    private void paused(SiteKey site, PlacedStructure structure, StructureStatus to) {
        notices.onStatusChange(new StructureStatusChangeEvent(site, structure, StructureStatus.UNDER_CONSTRUCTION, to));
    }

    @Test
    void ac45_membersInTheCampHearWhenAJobWaitsForACrew() {
        notices.onPlaced(new StructurePlacedEvent(SITE, structure(CrewRule.ID)));

        final ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(member).sendMessage(message.capture());
        final String text = message.getValue().toString();
        assertTrue(text.contains("clans.settler.crew.needed"), text);
        assertTrue(text.contains(CampStructures.WORKSHOP), text);
        assertTrue(text.contains("\"4\""), text);
        verify(visitor, never()).sendMessage(any(Component.class));
    }

    @Test
    void ac45_aJobPausedForItsCrewIsAnnounced() {
        paused(SITE, structure(CrewRule.ID), StructureStatus.PAUSED);
        verify(member).sendMessage(any(Component.class));
    }

    @Test
    void ac45_jobsNotWaitingForACrewAndOtherSitesAreNotAnnounced() {
        paused(SITE, structure(CrewRule.ID), StructureStatus.ACTIVE);
        paused(SITE, structure("siege"), StructureStatus.PAUSED);
        notices.onPlaced(new StructurePlacedEvent(SITE, structure(null)));
        final PlacedStructure noJob = structure(null);
        noJob.setJob(null);
        notices.onPlaced(new StructurePlacedEvent(SITE, noJob));
        notices.onPlaced(new StructurePlacedEvent(SiteKey.of("island", 42), structure(CrewRule.ID)));

        verify(member, never()).sendMessage(any(Component.class));
    }
}
