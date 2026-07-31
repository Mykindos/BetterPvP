package me.mykindos.betterpvp.clans.world.island;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IslandInstanceTest {

    private static IslandInstance newInstance() {
        final IslandTemplate template = new IslandTemplate("solo", Component.text("Solo"), "islands/solo", Material.GRASS_BLOCK);
        return new IslandInstance(UUID.randomUUID(), template, "islands/solo/abc12345");
    }

    @Test
    @DisplayName("a new instance is empty and its lastVacatedAt starts at its createdAt")
    void newInstanceIsEmpty() {
        final IslandInstance instance = newInstance();

        assertTrue(instance.isEmpty());
        assertEquals(instance.getCreatedAt(), instance.getLastVacatedAt());
    }

    @Test
    @DisplayName("adding an occupant makes the instance non-empty, removing the last one makes it empty again")
    void occupancyAddRemove() {
        final IslandInstance instance = newInstance();
        final UUID player = UUID.randomUUID();

        instance.addOccupant(player);
        assertFalse(instance.isEmpty());

        instance.removeOccupant(player);
        assertTrue(instance.isEmpty());
    }

    @Test
    @DisplayName("removing one of several occupants leaves the instance non-empty")
    void partialOccupantRemoval() {
        final IslandInstance instance = newInstance();
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();

        instance.addOccupant(first);
        instance.addOccupant(second);
        instance.removeOccupant(first);

        assertFalse(instance.isEmpty());
    }

    @Test
    @DisplayName("lastVacatedAt only updates when explicitly set, not on occupant changes")
    void lastVacatedAtIsExplicit() {
        final IslandInstance instance = newInstance();
        final long createdAt = instance.getCreatedAt();

        instance.addOccupant(UUID.randomUUID());
        assertEquals(createdAt, instance.getLastVacatedAt());

        instance.setLastVacatedAt(createdAt + 5000);
        assertEquals(createdAt + 5000, instance.getLastVacatedAt());
    }

    @Test
    @DisplayName("getShortId is the first 8 hex characters of the instance id")
    void shortIdIsPrefixOfId() {
        final IslandInstance instance = newInstance();
        assertEquals(instance.getId().toString().substring(0, 8), instance.getShortId());
    }
}
