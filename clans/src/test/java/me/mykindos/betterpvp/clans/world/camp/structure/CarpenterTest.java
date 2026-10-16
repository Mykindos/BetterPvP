package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.CampStore;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureStage;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The Carpenter as the code and the shipped camps.yml give it. */
class CarpenterTest {

    private final StructureCatalogue catalogue = new StructureCatalogue();
    private StructureType carpenter;

    @BeforeEach
    void setUp() throws Exception {
        final ExtendedYamlConfiguration yaml = new ExtendedYamlConfiguration();
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/configs/camps.yml")), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }
        final Clans clans = mock(Clans.class);
        when(clans.getConfig("camps")).thenReturn(yaml);
        when(clans.getReloadables()).thenReturn(new ArrayList<>());
        new CampStructures(new CampConfig(clans), catalogue, new CampUpgrades(mock(CampStore.class)));
        carpenter = catalogue.find(CampStructures.CARPENTER).orElseThrow();
    }

    @Test
    void theCarpenterIsTierTwoAndNeedsAWorkshop() {
        assertEquals(2, carpenter.getTier(), "tier 2 waits for a Great Hall at its second stage");
        assertEquals(Set.of(CampStructures.WORKSHOP), carpenter.getRequiredStructures());
        assertTrue(carpenter.getFlags().isDemolishable());
        assertTrue(carpenter.getFlags().isMovable());
    }

    @Test
    void theShippedFirstStageCostsTimeAndWorkforceAreTheIssues() {
        assertEquals(1, carpenter.getStages().size());
        final StructureStage first = carpenter.stage(0);
        assertEquals("camps/carpenter_1", first.getSchematic());
        assertEquals(64, first.getCost().get("wood"));
        assertEquals(32, first.getCost().get("stone"));
        assertEquals(Duration.ofMinutes(5), first.getBuildTime());
        assertEquals(4, first.getWorkforce(), "a crew needs the Tier 2 stage 1 Workforce threshold to start");
    }
}
