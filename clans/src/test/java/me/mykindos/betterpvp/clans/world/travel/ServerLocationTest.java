package me.mykindos.betterpvp.clans.world.travel;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerLocationTest {

    private static final String CURRENT_SERVER = "current-server";

    private Realm originalRealm;
    private MockedStatic<Bukkit> bukkitStatic;

    @BeforeEach
    void stubCurrentServer() {
        originalRealm = Core.getCurrentRealm();
        Core.setCurrentRealm(new Realm(1, new Server(1, CURRENT_SERVER), new Season(1, "test-season", LocalDate.now())));

        // UtilWorld.stringToLocation looks the world up via Bukkit.getWorld(); no world is loaded in this unit test,
        // matching the real "world not currently loaded" case that ServerLocation.toLocation() guards against.
        bukkitStatic = Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(() -> Bukkit.getWorld(anyString())).thenReturn(null);
    }

    @AfterEach
    void restoreCurrentServer() {
        Core.setCurrentRealm(originalRealm);
        bukkitStatic.close();
    }

    private static World mockWorld(String name) {
        final World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        return world;
    }

    @Test
    @DisplayName("serialize/parse round trip preserves server and coordinates")
    void serializeParseRoundTrip() {
        final World world = mockWorld("world_nether");
        final Location location = new Location(world, 10.0, 65.0, -20.0, 90.0f, 15.0f);
        final ServerLocation original = new ServerLocation("origin-server", location);

        final String serialized = original.serialize();
        assertTrue(serialized.startsWith("origin-server|"));

        final ServerLocation parsed = ServerLocation.parse(serialized);
        assertEquals("origin-server", parsed.getServer());
        assertEquals(10.0, parsed.getLocation().getX());
        assertEquals(65.0, parsed.getLocation().getY());
        assertEquals(-20.0, parsed.getLocation().getZ());
        assertEquals(90.0f, parsed.getLocation().getYaw());
        assertEquals(15.0f, parsed.getLocation().getPitch());
    }

    @Test
    @DisplayName("parse treats a legacy value with no server prefix as belonging to the current server")
    void parseLegacyValueDefaultsToCurrentServer() {
        final ServerLocation parsed = ServerLocation.parse("world, 1.0, 2.0, 3.0, 0.0, 0.0");
        assertEquals(CURRENT_SERVER, parsed.getServer());
        assertEquals(1.0, parsed.getLocation().getX());
    }

    @Test
    @DisplayName("isLocal is true only when the tagged server matches the current server")
    void isLocal() {
        final World world = mockWorld("world");
        final Location location = new Location(world, 0.0, 0.0, 0.0);

        assertTrue(new ServerLocation(CURRENT_SERVER, location).isLocal());
        assertFalse(new ServerLocation("other-server", location).isLocal());
    }

    @Test
    @DisplayName("local() tags a location with the current server")
    void localFactoryTagsCurrentServer() {
        final World world = mockWorld("world");
        final Location location = new Location(world, 0.0, 0.0, 0.0);

        final ServerLocation local = ServerLocation.local(location);
        assertEquals(CURRENT_SERVER, local.getServer());
    }

    @Test
    @DisplayName("toLocation is empty when the server is not local, even if the world is loaded")
    void toLocationEmptyWhenNotLocal() {
        final World world = mockWorld("world");
        final Location location = new Location(world, 0.0, 0.0, 0.0);
        final ServerLocation remote = new ServerLocation("other-server", location);

        assertTrue(remote.toLocation().isEmpty());
    }

    @Test
    @DisplayName("toLocation is empty when local but the world is not currently loaded")
    void toLocationEmptyWhenWorldAbsent() {
        final Location location = new Location(null, 0.0, 0.0, 0.0);
        final ServerLocation local = new ServerLocation(CURRENT_SERVER, location);

        assertTrue(local.toLocation().isEmpty());
    }

    @Test
    @DisplayName("toLocation returns the location when local and the world is loaded")
    void toLocationPresentWhenLocalAndLoaded() {
        final World world = mockWorld("world");
        final Location location = new Location(world, 5.0, 6.0, 7.0);
        final ServerLocation local = new ServerLocation(CURRENT_SERVER, location);

        final Optional<Location> resolved = local.toLocation();
        assertTrue(resolved.isPresent());
        assertEquals(location, resolved.get());
    }

    @Test
    @DisplayName("parse throws on malformed input rather than silently producing a bogus location")
    void parseMalformedInputThrows() {
        assertThrows(RuntimeException.class, () -> ServerLocation.parse("not-a-location"));
    }
}
