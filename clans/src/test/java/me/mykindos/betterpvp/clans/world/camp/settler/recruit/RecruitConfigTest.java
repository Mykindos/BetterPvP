package me.mykindos.betterpvp.clans.world.camp.settler.recruit;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.world.camp.settler.CampProfessions;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecruitConfigTest {

    private static RecruitConfig load(String yaml) throws Exception {
        final ExtendedYamlConfiguration settlers = new ExtendedYamlConfiguration();
        settlers.loadFromString(yaml);
        final Clans clans = mock(Clans.class);
        when(clans.getConfig("settlers")).thenReturn(settlers);
        return new RecruitConfig(clans, new CampProfessions(new ProfessionRegistry()));
    }

    private static String shipped() throws IOException {
        try (InputStream in = RecruitConfigTest.class.getClassLoader().getResourceAsStream("configs/settlers.yml")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void ac10_campProfessionsNoneAndAnyAreNotReported() throws Exception {
        assertTrue(load(shipped()).unknownProfessions().isEmpty());
    }

    @Test
    void ac10_unknownProfessionsAreReportedWhereTheyAreNamed() throws Exception {
        final RecruitConfig config = load("""
                arrivals:
                  profession-odds: {builder: 40, ghost: 10, none: 50}
                hiring:
                  profession-odds: {wizard: 1, farmer: 1}
                milestones:
                  5: {profession: ghost, rarity: common}
                  10: {profession: any, rarity: rare}
                  25: {profession: farmer, rarity: rare}
                """);

        assertEquals(Map.of(
                "arrivals.profession-odds", List.of("ghost"),
                "hiring.profession-odds", List.of("wizard"),
                "milestones.5", List.of("ghost")), config.unknownProfessions());
    }
}
