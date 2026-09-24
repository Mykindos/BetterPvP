package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCondition;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import me.mykindos.betterpvp.core.world.construction.StructurePosition;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpgradePiecesTest {

    private static final SiteKey CAMP = Camps.keyFor(5);
    private static final StructureUpgrade BOARD = new StructureUpgrade("board", 0, ResourceCost.NONE, Duration.ZERO,
            0, "camps/upgrades/board");

    private final Camps camps = mock(Camps.class);
    private final Player player = mock(Player.class);
    private final World world = mock(World.class);
    private final CampUpgrades upgrades = new CampUpgrades(mock(CampStore.class));
    private final AtomicInteger opened = new AtomicInteger();
    private PlacedStructure structure;

    @BeforeEach
    void setUp() {
        when(player.getWorld()).thenReturn(world);
        structure = new PlacedStructure(UUID.randomUUID(), CampStructures.BARRACKS, new StructurePosition(0, 64, 0, 0),
                StructureCondition.ACTIVE);
        upgrades.page("board", (viewer, camp, placed, previous) -> opened.incrementAndGet());
    }

    @Test
    void aMemberUsingAFittedPieceOpensItsPage() {
        when(camps.isMember(player, world)).thenReturn(true);
        structure.getUpgrades().put(0, "board");
        final StructurePieceUseEvent event = new StructurePieceUseEvent(player, CAMP, structure, BOARD);

        new UpgradePieces(camps, upgrades).onUse(event);

        assertTrue(event.isHandled());
        assertEquals(1, opened.get());
    }

    @Test
    void anOutsiderGetsNothing() {
        when(camps.isMember(player, world)).thenReturn(false);
        structure.getUpgrades().put(0, "board");
        final StructurePieceUseEvent event = new StructurePieceUseEvent(player, CAMP, structure, BOARD);

        new UpgradePieces(camps, upgrades).onUse(event);

        assertFalse(event.isHandled());
        assertEquals(0, opened.get());
    }
}
