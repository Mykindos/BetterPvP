package me.mykindos.betterpvp.core.world.site;

import me.mykindos.betterpvp.core.Core;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Whose instance of an owned site is whose")
class SiteOwnersTest {

    private static final String CATALOGUE = """
            spawn:
              world:
                adopt: "Spawn"
              lifecycle: PERMANENT
            camp:
              world:
                own: "camps/"
                from: "templates/camp"
              lifecycle: OWNED
            """;

    private SiteRegistry registry;
    private SiteOwners owners;
    private Player player;

    @BeforeEach
    void setUp() {
        registry = new SiteRegistry(mock(Core.class));
        registry.load(YamlConfiguration.loadConfiguration(new StringReader(CATALOGUE)));
        owners = new SiteOwners();
        player = mock(Player.class);
    }

    private Site site(String id) {
        return registry.get(id).orElseThrow();
    }

    @Test
    @DisplayName("a site nobody owns is the same one for everybody")
    void anUnownedSiteIsSharedByAll() {
        assertEquals(Optional.of(SiteKey.of("spawn")), owners.keyFor(site("spawn"), player));
    }

    @Test
    @DisplayName("an owned site with nothing registered has no instance to send anybody to")
    void anUnclaimedOwnedSiteHasNoInstance() {
        assertTrue(owners.keyFor(site("camp"), player).isEmpty());
    }

    @Test
    @DisplayName("an owned site names the owner the module resolved")
    void anOwnedSiteCarriesItsOwner() {
        owners.register("camp", who -> OptionalLong.of(42L));

        assertEquals(Optional.of(SiteKey.of("camp", 42L)), owners.keyFor(site("camp"), player));
    }

    @Test
    @DisplayName("somebody who belongs to no owner has nowhere of their own")
    void somebodyWithNoOwnerHasNowhere() {
        owners.register("camp", who -> OptionalLong.empty());

        assertTrue(owners.keyFor(site("camp"), player).isEmpty());
    }

    @Test
    @DisplayName("an owner that has chosen nothing leaves the world on disk alone")
    void nothingChosenLeavesTheWorldAlone() {
        owners.register("camp", who -> OptionalLong.of(42L));

        assertTrue(owners.templateFor(site("camp"), SiteKey.of("camp", 42L)).isEmpty(),
                "the site's own default must not read as a choice, or an unread record would rebuild a world");
    }

    @Test
    @DisplayName("what an owner chose is what their world is built from")
    void anOwnersChoiceIsWhatIsBuilt() {
        owners.register("camp", new SiteOwnership() {
            @Override
            public @NotNull OptionalLong ownerOf(@NotNull Player who) {
                return OptionalLong.of(42L);
            }

            @Override
            public @NotNull Optional<String> templateFor(@NotNull SiteKey key) {
                return Optional.of("templates/camps/shore");
            }
        });

        assertEquals(Optional.of("templates/camps/shore"),
                owners.templateFor(site("camp"), SiteKey.of("camp", 42L)));
    }
}
