package me.mykindos.betterpvp.clans.world.resource;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNodeDefinitionTest {

    private static YamlConfiguration baseConfig() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("archetype", "ore");
        config.set("match.name", "copper_mine");
        return config;
    }

    @Test
    @DisplayName("respawn as a number is parsed as a delay in seconds and is not one-shot")
    void numericRespawnIsParsedAsSeconds() {
        final YamlConfiguration config = baseConfig();
        config.set("respawn", 45);

        final ResourceNodeDefinition definition = ResourceNodeDefinition.from("copper_mine", config);

        assertNotNull(definition);
        assertEquals(45.0, definition.getRespawnSeconds());
        assertFalse(definition.isOneShot());
    }

    @Test
    @DisplayName("respawn 'none' or 'never', in any case, produces a one-shot node")
    void noneOrNeverProducesOneShot() {
        for (String value : new String[] {"none", "NONE", "None", "never", "NEVER", "Never"}) {
            final YamlConfiguration config = baseConfig();
            config.set("respawn", value);

            final ResourceNodeDefinition definition = ResourceNodeDefinition.from("copper_mine", config);

            assertNotNull(definition, "value: " + value);
            assertTrue(definition.isOneShot(), "value: " + value);
        }
    }

    @Test
    @DisplayName("an unparseable respawn value falls back to the default without throwing")
    void unparseableRespawnFallsBackToDefault() {
        final YamlConfiguration config = baseConfig();
        config.set("respawn", "not-a-number");

        final ResourceNodeDefinition definition = ResourceNodeDefinition.from("copper_mine", config);

        assertNotNull(definition);
        assertEquals(60.0, definition.getRespawnSeconds());
        assertFalse(definition.isOneShot());
    }

    @Test
    @DisplayName("a missing respawn value falls back to the default without throwing")
    void missingRespawnFallsBackToDefault() {
        final YamlConfiguration config = baseConfig();

        final ResourceNodeDefinition definition = ResourceNodeDefinition.from("copper_mine", config);

        assertNotNull(definition);
        assertEquals(60.0, definition.getRespawnSeconds());
        assertFalse(definition.isOneShot());
    }

    @Test
    @DisplayName("from returns null when the archetype key is missing")
    void missingArchetypeReturnsNull() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("match.name", "copper_mine");

        assertEquals(null, ResourceNodeDefinition.from("copper_mine", config));
    }

    @Test
    @DisplayName("from returns null when no region selector is present")
    void missingMatchSelectorReturnsNull() {
        final YamlConfiguration config = new YamlConfiguration();
        config.set("archetype", "ore");

        assertEquals(null, ResourceNodeDefinition.from("copper_mine", config));
    }
}
