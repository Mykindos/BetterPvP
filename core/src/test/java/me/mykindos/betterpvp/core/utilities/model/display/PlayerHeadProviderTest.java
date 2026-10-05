package me.mykindos.betterpvp.core.utilities.model.display;

import com.github.benmanes.caffeine.cache.Ticker;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerHeadProviderTest {

    private static final int SCALE = 3;
    private static final int TOP = 16;

    private final Deque<Runnable> pending = new ArrayDeque<>();
    private final AtomicLong nanos = new AtomicLong();
    private final Ticker ticker = nanos::get;

    private URL skinUrl;

    @BeforeEach
    void setUp() throws Exception {
        skinUrl = URI.create("http://textures.minecraft.net/texture/test").toURL();
    }

    private PlayerHeadProvider provider(PlayerHeadProvider.SkinDownloader downloader) {
        return new PlayerHeadProvider(pending::add, downloader, ticker);
    }

    private void runPending() {
        while (!pending.isEmpty()) {
            pending.poll().run();
        }
    }

    private void advance(Duration duration) {
        nanos.addAndGet(duration.toNanos());
    }

    private Player player(URL skin) {
        final Player player = mock(Player.class, RETURNS_DEEP_STUBS);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getPlayerProfile().getTextures().getSkin()).thenReturn(skin);
        return player;
    }

    private static BufferedImage skin(int rgb) {
        final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    @Test
    @DisplayName("AC2: the placeholder head is painted while the skin loads and when there is no skin URL")
    void ac2_placeholderWhileLoadingOrWithoutSkin() {
        final PlayerHeadProvider heads = provider(skin -> skin(0xFFFF0000));
        final Component placeholder = heads.placeholder(SCALE, TOP);
        assertNotEquals(Component.empty(), placeholder, "the placeholder is a painted head");

        final Player loading = player(skinUrl);
        assertEquals(placeholder, heads.head(loading, SCALE, TOP), "skin still downloading");

        final Player noSkin = player(null);
        assertEquals(placeholder, heads.head(noSkin, SCALE, TOP), "player has no skin URL");
    }

    @Test
    @DisplayName("AC3: an expired head stays painted until the refreshed head is ready")
    void ac3_keepsPreviousHeadWhileRefreshing() {
        final Deque<BufferedImage> skins = new ArrayDeque<>();
        skins.add(skin(0xFFFF0000));
        skins.add(skin(0xFF0000FF));
        final PlayerHeadProvider heads = provider(skin -> skins.poll());
        final Player player = player(skinUrl);

        heads.head(player, SCALE, TOP);
        runPending();
        final Component first = heads.head(player, SCALE, TOP);
        assertNotNull(first);
        assertNotEquals(heads.placeholder(SCALE, TOP), first);

        advance(Duration.ofMinutes(6));
        assertEquals(first, heads.head(player, SCALE, TOP), "expired head is kept while the refresh runs");

        runPending();
        final Component refreshed = heads.head(player, SCALE, TOP);
        assertNotNull(refreshed);
        assertNotEquals(first, refreshed, "refreshed head replaces the previous one once ready");
    }

    @Test
    @DisplayName("AC4: a failed download keeps the placeholder and is retried later")
    void ac4_failedDownloadKeepsPlaceholderAndRetries() {
        final AtomicInteger attempts = new AtomicInteger();
        final PlayerHeadProvider heads = provider(skin -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IOException("simulated download failure");
            }
            return skin(0xFF00FF00);
        });
        final Player player = player(skinUrl);
        final Component placeholder = heads.placeholder(SCALE, TOP);

        heads.head(player, SCALE, TOP);
        runPending();
        assertEquals(1, attempts.get());
        assertEquals(placeholder, heads.head(player, SCALE, TOP), "placeholder stays after a failure");

        advance(Duration.ofMinutes(5));
        heads.head(player, SCALE, TOP);
        runPending();
        assertEquals(2, attempts.get(), "the download is retried");

        final Component loaded = heads.head(player, SCALE, TOP);
        assertNotNull(loaded);
        assertNotEquals(placeholder, loaded, "the retried head replaces the placeholder");
    }
}
