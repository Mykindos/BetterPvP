package me.mykindos.betterpvp.clans.world.island;

import com.google.inject.Provider;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class IslandInstanceManagerTest {

    @Mock
    private IslandWorldProvisioner provisioner;

    @Mock
    private InstanceAllocationPolicy allocationPolicy;

    @Mock
    private IslandInstanceRepository repository;

    @Mock
    private Provider<IslandWarmPool> warmPoolProvider;

    private IslandInstanceManager manager;

    private static IslandTemplate template(String key) {
        return new IslandTemplate(key, Component.text(key), "islands/" + key, Material.GRASS_BLOCK);
    }

    private static IslandInstance register(IslandInstanceManager manager, UUID id) {
        final IslandInstance instance = new IslandInstance(id, template("solo"), "islands/solo/" + id.toString().substring(0, 8));
        manager.register(instance);
        return instance;
    }

    @BeforeEach
    void setUp() {
        manager = new IslandInstanceManager(provisioner, allocationPolicy, repository, warmPoolProvider);
    }

    @Test
    @DisplayName("matchIdPrefix returns no matches when nothing shares the prefix")
    void zeroMatches() {
        register(manager, UUID.fromString("11111111-1111-1111-1111-111111111111"));

        assertTrue(manager.matchIdPrefix("ffffffff").isEmpty());
    }

    @Test
    @DisplayName("matchIdPrefix returns the single instance when the prefix is unique")
    void uniqueMatch() {
        final IslandInstance instance = register(manager, UUID.fromString("11111111-1111-1111-1111-111111111111"));
        register(manager, UUID.fromString("22222222-2222-2222-2222-222222222222"));

        final List<IslandInstance> matches = manager.matchIdPrefix("1111");
        assertEquals(1, matches.size());
        assertEquals(instance, matches.get(0));
    }

    @Test
    @DisplayName("matchIdPrefix returns every instance sharing an ambiguous prefix")
    void ambiguousMatch() {
        register(manager, UUID.fromString("11111111-1111-1111-1111-111111111111"));
        register(manager, UUID.fromString("11112222-2222-2222-2222-222222222222"));

        final List<IslandInstance> matches = manager.matchIdPrefix("1111");
        assertEquals(2, matches.size());
    }

    @Test
    @DisplayName("matchIdPrefix is case-insensitive")
    void caseInsensitiveMatch() {
        final IslandInstance instance = register(manager, UUID.fromString("abcdef11-1111-1111-1111-111111111111"));

        assertEquals(List.of(instance), manager.matchIdPrefix("ABCDEF"));
    }
}
