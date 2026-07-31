package me.mykindos.betterpvp.clans.world.resource;

import dev.brauw.mapper.region.Region;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceNodeLoaderKeyTest {

    @Test
    @DisplayName("sanitizeForKey lowercases and replaces every character outside the Adventure Key charset")
    void sanitizeForKeyProducesLegalKeyCharacters() {
        assertEquals("islands_solo_abc12345", ResourceNodeLoader.sanitizeForKey("islands/solo/ABC12345"));
        assertEquals("world_one__chunk_4_-2", ResourceNodeLoader.sanitizeForKey("World One: chunk 4,-2"));
    }

    @Test
    @DisplayName("nodeKey namespaces by world name so worlds cloned from the same template never collide")
    void nodeKeyNamespacesByWorld() {
        final UUID regionId = UUID.randomUUID();
        final Region region = mock(Region.class);
        when(region.getId()).thenReturn(regionId);

        final World worldOne = mock(World.class);
        when(worldOne.getName()).thenReturn("islands/solo/aaaaaaaa");
        final World worldTwo = mock(World.class);
        when(worldTwo.getName()).thenReturn("islands/solo/bbbbbbbb");

        final Key keyOne = ResourceNodeLoader.nodeKey(region, worldOne);
        final Key keyTwo = ResourceNodeLoader.nodeKey(region, worldTwo);

        assertNotEquals(keyOne, keyTwo);
        assertEquals("clans", keyOne.namespace());
    }

    @Test
    @DisplayName("nodeKey is a legal Adventure Key even for a world name with illegal characters")
    void nodeKeyIsAlwaysLegal() {
        final Region region = mock(Region.class);
        when(region.getId()).thenReturn(UUID.randomUUID());

        final World world = mock(World.class);
        when(world.getName()).thenReturn("Weird World: 1!");

        Key.key(ResourceNodeLoader.nodeKey(region, world).asString());
    }
}
