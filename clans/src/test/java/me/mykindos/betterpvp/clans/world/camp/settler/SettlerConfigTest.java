package me.mykindos.betterpvp.clans.world.camp.settler;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.wage.WageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The PRD's wage and morale numbers, as the shipped settlers.yml gives them. */
class SettlerConfigTest {

    private final ExtendedYamlConfiguration yaml = new ExtendedYamlConfiguration();
    private SettlerConfig config;

    @BeforeEach
    void setUp() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream("/configs/settlers.yml")), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }
        final Clans clans = mock(Clans.class);
        when(clans.getConfig("settlers")).thenReturn(yaml);
        when(clans.getReloadables()).thenReturn(new ArrayList<>());
        config = new SettlerConfig(clans);
    }

    private static double rate(WageModel model, SettlerRarity rarity, boolean working) {
        final Settler builder = new Settler();
        builder.setProfession("builder");
        builder.setRarity(rarity);
        return model.perHour(builder, working);
    }

    @Test
    void ac2_theShippedFixedRatesAreThePrds() {
        final Map<SettlerRarity, Double> prd = Map.of(SettlerRarity.COMMON, 300.0, SettlerRarity.UNCOMMON, 600.0,
                SettlerRarity.RARE, 1_200.0, SettlerRarity.LEGENDARY, 2_500.0);
        prd.forEach((rarity, coins) -> {
            assertEquals(coins, rate(config.getWageModel(), rarity, false));
            assertEquals(coins, rate(config.getWageModel(), rarity, true));
        });
    }

    @Test
    void ac3_theShippedIdleAndWorkingRatesAreThePrds() {
        yaml.set("wages.model", "idle-working");
        config.reload();

        assertEquals(150, rate(config.getWageModel(), SettlerRarity.COMMON, false));
        assertEquals(450, rate(config.getWageModel(), SettlerRarity.COMMON, true));
    }

    @Test
    void ac15_theShippedStrikeLimitIs72Hours() {
        assertEquals(Duration.ofHours(72), config.getStrikeLimit());
    }

    @Test
    void ac19_theShippedLeaveLineIsBelowMinus40For48Hours() {
        assertEquals(-40, config.morale("leave-below", Double.NaN));
        assertEquals(48, config.morale("leave-after-hours", Double.NaN));
    }

    @Test
    void ac21_theShippedFoodCapIs30() {
        assertEquals(30, config.morale("food-max", Double.NaN));
    }

    @Test
    void ac22_theShippedUnpaidPenaltyIsMinus25() {
        assertEquals(-25, config.morale("unpaid", Double.NaN));
    }

    @Test
    void ac23_theShippedIdleNumbersAreThePrds() {
        assertEquals(24, config.morale("idle-after-hours", Double.NaN));
        assertEquals(-1, config.morale("idle-per-hour", Double.NaN));
        assertEquals(-30, config.morale("idle-floor", Double.NaN));
    }

    @Test
    void ac24_theShippedDismissalNumbersAreThePrds() {
        assertEquals(-10, config.morale("dismissal", Double.NaN));
        assertEquals(48, config.morale("dismissal-hours", Double.NaN));
        assertEquals(-30, config.morale("dismissal-floor", Double.NaN));
    }
}
