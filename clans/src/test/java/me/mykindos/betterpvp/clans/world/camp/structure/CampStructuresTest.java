package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampConstruction;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.world.construction.ResourceCost;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.construction.StructureUpgrade;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampStructuresTest {

    private final CampConfig config = mock(CampConfig.class);
    private final StructureCatalogue catalogue = new StructureCatalogue();
    private final CampUpgrades upgrades = new CampUpgrades(mock(CampStore.class));

    @Test
    void everyTierOneStructureIsRegistered() {
        when(config.structure(anyString())).thenReturn(Optional.empty());
        new CampStructures(config, catalogue, upgrades);

        assertEquals(5, catalogue.all().size());
        catalogue.all().forEach(type -> assertEquals(1, type.getTier()));
    }

    @Test
    void theHallBarracksAndDockCannotBeDemolishedAndTheDockNeverMoves() {
        when(config.structure(anyString())).thenReturn(Optional.empty());
        new CampStructures(config, catalogue, upgrades);

        assertFalse(find(CampConstruction.GREAT_HALL).getFlags().isDemolishable());
        assertFalse(find(CampStructures.BARRACKS).getFlags().isDemolishable());
        assertFalse(find(CampStructures.DOCK).getFlags().isDemolishable());
        assertFalse(find(CampStructures.DOCK).getFlags().isMovable());
        assertTrue(find(CampStructures.DOCK).getFlags().isStartsBroken());
        assertTrue(find(CampStructures.STOREHOUSE).getFlags().isDemolishable());
    }

    @Test
    void everythingButTheHallNeedsTheHallFirst() {
        when(config.structure(anyString())).thenReturn(Optional.empty());
        new CampStructures(config, catalogue, upgrades);

        assertTrue(find(CampConstruction.GREAT_HALL).getRequiredStructures().isEmpty());
        assertEquals(Set.of(CampConstruction.GREAT_HALL), find(CampStructures.WORKSHOP).getRequiredStructures());
    }

    @Test
    void numbersComeFromTheConfig() {
        final CampConfig.StructureNumbers numbers = new CampConfig.StructureNumbers(
                List.of(new StructureStage("camps/storehouse_1", ResourceCost.of(Map.of("wood", 120)),
                        Duration.ofMinutes(15))),
                ResourceCost.NONE, Duration.ZERO, ResourceCost.of(Map.of("wood", 40)), Duration.ofMinutes(3),
                0.25, Material.CHEST, Map.of("tool_rack", new CampConfig.UpgradeNumbers(
                        ResourceCost.of(Map.of("iron", 20)), Duration.ofMinutes(10), 2, "camps/upgrades/tool_rack",
                        Material.IRON_AXE, Map.of())));
        when(config.structure(anyString())).thenReturn(Optional.empty());
        when(config.structure(CampStructures.STOREHOUSE)).thenReturn(Optional.of(numbers));
        new CampStructures(config, catalogue, upgrades);

        final StructureType storehouse = find(CampStructures.STOREHOUSE);
        assertEquals(120, storehouse.stage(0).getCost().get("wood"));
        assertEquals(Duration.ofMinutes(3), storehouse.getRepairTime());
        assertEquals(0.25, storehouse.getFlags().getDemolishRefund(), 1e-9);
        assertTrue(storehouse.getFlags().isDemolishable(), "config never changes what the code says it allows");
    }

    @Test
    void aStructureOffersOnlyTheUpgradesDeclaredForItWithTheirNumbersFromTheConfig() {
        final CampConfig.StructureNumbers numbers = new CampConfig.StructureNumbers(
                List.of(new StructureStage("camps/workshop_1", ResourceCost.NONE, Duration.ZERO)),
                ResourceCost.NONE, Duration.ZERO, ResourceCost.NONE, Duration.ZERO, 0, Material.CRAFTING_TABLE,
                Map.of("tool_rack", new CampConfig.UpgradeNumbers(ResourceCost.of(Map.of("iron", 20)),
                        Duration.ofMinutes(10), 2, "camps/upgrades/tool_rack", Material.IRON_AXE, Map.of()),
                        "not_declared", new CampConfig.UpgradeNumbers(ResourceCost.NONE, Duration.ZERO, 0, null,
                                Material.ANVIL, Map.of())));
        when(config.structure(anyString())).thenReturn(Optional.empty());
        when(config.structure(CampStructures.WORKSHOP)).thenReturn(Optional.of(numbers));
        upgrades.declare(CampStructures.WORKSHOP, "tool_rack", 1);
        upgrades.declare(CampStructures.WORKSHOP, "later", 2);
        new CampStructures(config, catalogue, upgrades);

        final List<StructureUpgrade> offered = find(CampStructures.WORKSHOP).getUpgrades();
        assertEquals(List.of("tool_rack", "later"), offered.stream().map(StructureUpgrade::getId).toList());
        assertEquals(0, offered.getFirst().getStage(), "stages are declared counting from 1");
        assertEquals(20, offered.getFirst().getCost().get("iron"));
        assertEquals("camps/upgrades/tool_rack", offered.getFirst().getPiece());
        assertTrue(offered.get(1).getCost().isFree(), "an upgrade missing from the config is free");
        assertTrue(find(CampStructures.BARRACKS).getUpgrades().isEmpty());
    }

    private StructureType find(String id) {
        return catalogue.find(id).orElseThrow();
    }
}
