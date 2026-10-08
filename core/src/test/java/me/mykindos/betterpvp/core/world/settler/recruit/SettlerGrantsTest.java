package me.mykindos.betterpvp.core.world.settler.recruit;

import me.mykindos.betterpvp.core.world.settler.Profession;
import me.mykindos.betterpvp.core.world.settler.ProfessionRegistry;
import me.mykindos.betterpvp.core.world.settler.RarityNumbers;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerAction;
import me.mykindos.betterpvp.core.world.settler.SettlerGenerator;
import me.mykindos.betterpvp.core.world.settler.SettlerLook;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import me.mykindos.betterpvp.core.world.settler.SettlerResult;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import me.mykindos.betterpvp.core.world.settler.SettlerSite;
import me.mykindos.betterpvp.core.world.settler.SettlerTable;
import me.mykindos.betterpvp.core.world.settler.SettlerTemplate;
import me.mykindos.betterpvp.core.world.settler.TraitRegistry;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SettlerGrantsTest {

    private static final SiteKey SITE = SiteKey.of("outpost", 3);

    private final FakeSite site = new FakeSite();
    private MockedStatic<Bukkit> bukkit;
    private SettlerGrants grants;

    @BeforeEach
    void setUp() {
        bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

        final ProfessionRegistry professions = new ProfessionRegistry();
        professions.register(Profession.construction("builder", "builder", List.of()));
        final SettlerService service = new SettlerService(professions);
        service.register("outpost", site);
        grants = new SettlerGrants(service, new SettlerGenerator(professions, new TraitRegistry()));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private static String reason(SettlerResult result) {
        assertFalse(result.isSuccess(), "expected a refusal");
        return ((TranslatableComponent) result.getReason()).key();
    }

    private static SettlerTemplate prisoner() {
        return SettlerTemplate.builder()
                .rarity(SettlerRarity.RARE)
                .profession("builder")
                .source("dungeon")
                .historyArg("the Sunken Keep")
                .build();
    }

    @Test
    void ac4_aGrantRollsFromTheTemplateAndTheSitesOwnTable() {
        final SettlerResult result = grants.grant(SITE, prisoner());

        assertTrue(result.isSuccess());
        final Settler settler = result.getSettler();
        assertEquals(SettlerRarity.RARE, settler.getRarity());
        assertEquals("builder", settler.getProfession());
        assertEquals("history.dungeon", settler.getHistory());
        assertEquals(List.of("the Sunken Keep"), settler.getHistoryArgs());
        assertEquals("Mirel Salt", settler.getName());
        assertTrue(site.roster.find(settler.getId()).isPresent(), "added through the service");
    }

    @Test
    void ac4_aSiteWithNoTableIsNotLoaded() {
        site.table = null;
        assertEquals("core.settler.not_loaded", reason(grants.grant(SITE, prisoner())));
        assertEquals(0, site.roster.size());
    }

    @Test
    void ac4_aFullSiteRefusesWithItsCapAndNobodyJoins() {
        site.population = 1;
        assertTrue(grants.grant(SITE, prisoner()).isSuccess());

        assertEquals("core.settler.population_full", reason(grants.grant(SITE, prisoner())));
        assertEquals(1, site.roster.size());
    }

    private static final class FakeSite implements SettlerSite {

        private final Roster roster = new Roster();
        private SettlerTable table = new SettlerTable(Map.of(SettlerRarity.RARE, new RarityNumbers(0, 1.5, 1.6, 0)),
                List.of("Mirel"), List.of("Salt"), Map.of("dungeon", List.of("history.dungeon")));
        private int population = 10;

        @Override
        public Optional<Roster> roster(SiteKey site) {
            return Optional.of(roster);
        }

        @Override
        public void changed(SiteKey site) {
        }

        @Override
        public int populationCap(SiteKey site) {
            return population;
        }

        @Override
        public OptionalInt workingCap(SiteKey site, String profession) {
            return OptionalInt.empty();
        }

        @Override
        public boolean allows(Player player, SiteKey site, SettlerAction action) {
            return true;
        }

        @Override
        public SettlerLook look(SiteKey site, Settler settler) {
            return new SettlerLook("model", null, "idle", "walk", "work", 1);
        }

        @Override
        public Optional<SettlerTable> table(SiteKey site) {
            return Optional.ofNullable(table);
        }
    }
}
