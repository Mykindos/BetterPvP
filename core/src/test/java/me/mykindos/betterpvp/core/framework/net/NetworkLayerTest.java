package me.mykindos.betterpvp.core.framework.net;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Choosing what carries the network")
class NetworkLayerTest {

    private final Map<String, String> config = new HashMap<>();

    private NetworkTransports transports;
    private NetworkLayer layer;

    @BeforeEach
    void setUp() {
        final ExtendedYamlConfiguration yaml = mock(ExtendedYamlConfiguration.class);
        when(yaml.getOrSaveString(anyString(), anyString()))
                .thenAnswer(call -> config.getOrDefault(call.getArgument(0, String.class),
                        call.getArgument(1, String.class)));

        final Core core = mock(Core.class);
        when(core.getConfig()).thenReturn(yaml);

        // Redis says it is not configured, which is the shape every unavailable implementation takes.
        final RedisConnection redis = mock(RedisConnection.class);
        when(redis.isUsable()).thenReturn(false);

        transports = new NetworkTransports();
        layer = new NetworkLayer(core, transports, redis,
                () -> mock(VelocityMessageBus.class),
                LocalSiteDirectory::new,
                () -> mock(ProxyPlayerTransfer.class));
    }

    @Test
    @DisplayName("with nothing configured, each seam takes the best that can run")
    void autoTakesWhatIsAvailable() {
        assertTrue(layer.directory() instanceof LocalSiteDirectory, "Redis is not usable, so the local one is left");
        assertTrue(layer.bus() instanceof VelocityMessageBus);
    }

    @Test
    @DisplayName("another network's implementation is used when config names it")
    void aRegisteredNameWins() {
        final MessageBus theirs = mock(MessageBus.class);
        final PlayerTransfer theirTransfer = mock(PlayerTransfer.class);
        transports.registerBus("theirs", () -> theirs);
        transports.registerTransfer("theirs", () -> theirTransfer);
        config.put("core.network.bus", "theirs");
        config.put("core.network.transfer", "theirs");

        assertSame(theirs, layer.bus());
        assertSame(theirTransfer, layer.transfer());
    }

    @Test
    @DisplayName("a name is matched however it was written")
    void namesAreMatchedLoosely() {
        final MessageBus theirs = mock(MessageBus.class);
        transports.registerBus("TheirBus", () -> theirs);
        config.put("core.network.bus", "  theirbus ");

        assertSame(theirs, layer.bus());
    }

    @Test
    @DisplayName("a name nobody registered falls back rather than leaving the server without a bus")
    void anUnknownNameFallsBack() {
        config.put("core.network.bus", "nothing-registered-this");

        assertTrue(layer.bus() instanceof VelocityMessageBus);
    }

    @Test
    @DisplayName("an implementation that cannot run here is passed over")
    void anUnavailableImplementationIsPassedOver() {
        transports.registerBus("theirs", () -> null);
        config.put("core.network.bus", "theirs");

        assertTrue(layer.bus() instanceof VelocityMessageBus);
    }

    @Test
    @DisplayName("each seam is resolved once and kept")
    void seamsAreResolvedOnce() {
        assertSame(layer.bus(), layer.bus());
        assertSame(layer.directory(), layer.directory());
        assertSame(layer.transfer(), layer.transfer());
    }

    @Test
    @DisplayName("the seams are chosen separately, so one can be replaced without the others")
    void seamsAreIndependent() {
        final PlayerTransfer theirTransfer = mock(PlayerTransfer.class);
        transports.registerTransfer("theirs", () -> theirTransfer);
        config.put("core.network.transfer", "theirs");

        assertSame(theirTransfer, layer.transfer());
        assertTrue(layer.bus() instanceof VelocityMessageBus, "the bus should be untouched");
        assertTrue(layer.directory() instanceof LocalSiteDirectory, "the directory should be untouched");
    }

    @Test
    @DisplayName("a directory only this server can read does not claim to be shared")
    void localDirectoryIsNotShared() {
        assertTrue(layer.directory().isAvailable());
        assertSame(false, layer.directory().isShared());
    }

    @Test
    @DisplayName("naming one seam does not name the others")
    void namingOneSeamLeavesTheRest() {
        final SiteDirectory theirs = mock(SiteDirectory.class);
        transports.registerDirectory("theirs", () -> theirs);
        config.put("core.network.directory", "theirs");

        assertSame(theirs, layer.directory());
        assertTrue(layer.bus() instanceof VelocityMessageBus);
    }
}
