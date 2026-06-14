package me.mykindos.betterpvp.core.utilities.model.display;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Value;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a player's skin head as a block of tinted 1px glyphs, the same trick the combat
 * {@code StatusBar} uses to colour its bar pieces: a single white pixel glyph ({@code \uE001} in the
 * {@code offset/*} fonts) re-emitted once per skin pixel, each tinted with that pixel's colour. The
 * client multiplies the white texture by the component colour, so the head paints itself out of text.
 *
 * <p>The whole pipeline runs once per player and is cached: the skin PNG is fetched off-thread, the
 * front face and hat overlay are flattened into one 8&times;8 colour grid, and the grid is built into a
 * <em>net-zero horizontal advance</em> {@link Component} (it rewinds its own cursor, so a caller drops
 * it in at any X like a single glyph). Per tick the HUD only appends that finished component.
 */
@Singleton
@CustomLog
public class PlayerHeadProvider {

    private static final int GRID = 8;                 // a head face is 8x8 skin pixels
    private static final char PIXEL = '\uE001';        // the 1px white square in the offset/* fonts
    // Skin-texture regions (64x64 and legacy 64x32 both carry these in the top-left quadrant).
    private static final int FACE_X = 8, FACE_Y = 8;   // front of the head
    private static final int HAT_X = 40;               // hat/helmet overlay (same Y as the face)
    private static final int HAT_ALPHA = 128;          // overlay the hat pixel only when it's ~opaque
    // The 1px-left cursor pay-back that tiles pixels edge-to-edge, hoisted to one shared instance.
    private static final Component SPACE_BACK = Component.translatable("space.-1").font(Resources.Font.SPACE);

    private final Core core;

    // Finished, ready-to-append head blocks keyed by (player, scale, top) so a change to either layout
    // input renders a fresh head instead of returning a stale one. Expiry doubles as a skin-change refresh.
    private final Cache<HeadKey, Component> cache = Caffeine.newBuilder()
            .maximumSize(512)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
    // Keys whose skin is mid-fetch, so a head rendered every tick kicks off exactly one load per layout.
    private final Set<HeadKey> inFlight = ConcurrentHashMap.newKeySet();

    @Inject
    public PlayerHeadProvider(Core core) {
        this.core = core;
    }

    /**
     * The player's head as a net-zero-advance component, or empty while the skin is still loading (the
     * caller draws its frame without the head that tick and it pops in once ready).
     *
     * @param scale on-screen size multiplier per skin pixel (e.g. 4 &rarr; a 32px head)
     * @param top   vertical offset of the head's top row, as an {@code offset/down_N} font index
     */
    public Optional<Component> head(Player player, int scale, int top) {
        final HeadKey key = new HeadKey(player.getUniqueId(), scale, top);
        final Component cached = cache.getIfPresent(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        load(player, key);
        return Optional.empty();
    }

    private void load(Player player, HeadKey key) {
        if (!inFlight.add(key)) {
            return; // already loading for this key
        }

        final URL skin;
        try {
            skin = player.getPlayerProfile().getTextures().getSkin();
        } catch (Exception e) {
            inFlight.remove(key);
            return;
        }
        if (skin == null) {
            inFlight.remove(key); // no skin yet; retried cheaply on the next render
            return;
        }

        UtilServer.runTaskAsync(core, () -> {
            try {
                final TextColor[] grid = readGrid(skin);
                if (grid != null) {
                    cache.put(key, build(grid, key.getScale(), key.getTop()));
                }
            } catch (Exception e) {
                log.warn("Failed to load skin head for {}", key.getPlayer(), e).submit();
            } finally {
                inFlight.remove(key);
            }
        });
    }

    /** Download the skin and flatten its head face + hat into one 8&times;8 colour grid. */
    private TextColor[] readGrid(URL skin) throws Exception {
        final URLConnection conn = skin.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        final BufferedImage img;
        try (InputStream in = conn.getInputStream()) {
            img = ImageIO.read(in);
        }
        if (img == null) {
            return null;
        }

        final TextColor[] grid = new TextColor[GRID * GRID];
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                final int face = img.getRGB(FACE_X + x, FACE_Y + y);
                final int hat = img.getRGB(HAT_X + x, FACE_Y + y);
                final int argb = (hat >>> 24) >= HAT_ALPHA ? hat : face;
                grid[y * GRID + x] = TextColor.color(HudAnchor.LEFT.mark(argb & 0xFFFFFF));
            }
        }
        return grid;
    }

    /**
     * Build the head block: each skin pixel becomes a {@code scale}px square — {@code scale} stacked
     * sub-rows (a distinct {@code offset/down_N} font per Y), each laying the 1px glyph {@code scale}
     * times across with a per-glyph {@code space(-1)} so pixels tile edge-to-edge. Because this rides the
     * boss-bar title, the font is hoisted onto a per-sub-row parent and the colour onto a per-run parent
     * (children inherit both), so each long token serialises once instead of once per pixel. Each sub-row
     * rewinds its width, so the whole block has net-zero advance.
     */
    private Component build(TextColor[] grid, int scale, int top) {
        final String px = String.valueOf(PIXEL);
        final FontCanvas canvas = new FontCanvas();
        for (int row = 0; row < GRID; row++) {
            for (int sub = 0; sub < scale; sub++) {
                final TextComponent.Builder line =
                        Component.text().font(FontCanvas.font("offset/down_" + (top + row * scale + sub)));
                int col = 0;
                while (col < GRID) {
                    final TextColor color = grid[row * GRID + col];
                    int run = 1;
                    while (col + run < GRID && color.equals(grid[row * GRID + col + run])) {
                        run++;
                    }
                    final TextComponent.Builder pixels = Component.text().color(color);
                    for (int i = 0; i < run * scale; i++) {
                        pixels.append(Component.text(px)).append(SPACE_BACK);
                    }
                    line.append(pixels.build());
                    col += run;
                }
                canvas.append(line.build()).space(-(GRID * scale)); // rewind row width -> net-zero
            }
        }
        return canvas.build().shadowColor(ShadowColor.none());
    }

    /** Cache identity for a rendered head: the same skin laid out at a different scale or top is a distinct entry. */
    @Value
    private static class HeadKey {
        UUID player;
        int scale;
        int top;
    }
}
