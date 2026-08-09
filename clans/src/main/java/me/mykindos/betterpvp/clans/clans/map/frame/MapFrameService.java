package me.mykindos.betterpvp.clans.clans.map.frame;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.ClanMapService;
import me.mykindos.betterpvp.clans.clans.map.MapHandler;
import me.mykindos.betterpvp.clans.clans.map.MapZoneStyleRegistry;
import me.mykindos.betterpvp.clans.clans.map.claim.ClaimEntry;
import me.mykindos.betterpvp.clans.clans.map.claim.ClanClaimIndex;
import me.mykindos.betterpvp.clans.clans.map.data.MapBackground;
import me.mykindos.betterpvp.clans.clans.map.data.MapFill;
import me.mykindos.betterpvp.clans.clans.map.data.MapFillStyle;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.map.data.MapZoneStyle;
import me.mykindos.betterpvp.clans.clans.map.terrain.MapTerrainCache;
import me.mykindos.betterpvp.clans.clans.map.terrain.MapTerrainService;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Composes the 128×128 byte frame each viewer's map shows, entirely off the main thread.
 * <p>
 * The terrain half of a frame depends only on {@code (world, tile, zoom)} — never on who is looking — so it is built
 * once and shared by every viewer standing on the same tile. Only the clan overlay is per-viewer, and it is drawn over
 * a copy. The result is published to {@link MapSettings#getFrame()}, leaving the render call on the main thread with
 * nothing to do but copy bytes onto the canvas.
 */
@Singleton
public class MapFrameService {

    private static final int MAP_SIZE = MapSettings.MAP_SIZE;
    private static final int MAP_CENTER = MAP_SIZE / 2;
    private static final int CHUNK_WIDTH = 16;
    private static final String CLAN_TERRITORY_TAG = "clan_territory";
    private static final byte NO_DATA = 0;
    /** How often the opportunistic terrain fill runs, in ticks. */
    private static final int FILL_INTERVAL_TICKS = 4;

    private final Clans clans;
    private final MapHandler mapHandler;
    private final MapTerrainService terrainService;
    private final ClanClaimIndex claimIndex;
    private final ClanMapService clanMapService;
    private final MapZoneStyleRegistry styleRegistry;

    @Inject
    @Config(path = "clans.map.background", defaultValue = "CASCADE")
    private String backgroundModeName;

    @Inject
    @Config(path = "clans.map.unknownColor", defaultValue = "#3a6a9a")
    private String unknownColorHex;

    private Byte unknownColorId;

    /** Terrain tiles keyed by position, zoom and terrain generation, so a stale tile simply misses. */
    private final Cache<TileKey, byte[]> tiles = Caffeine.newBuilder()
            .maximumSize(512)
            .expireAfterAccess(Duration.ofSeconds(15))
            .build();

    private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
        final Thread thread = new Thread(runnable, "BPvP-Map-Frame");
        thread.setDaemon(true);
        return thread;
    });

    @Inject
    public MapFrameService(Clans clans, MapHandler mapHandler, MapTerrainService terrainService,
                           ClanClaimIndex claimIndex, ClanMapService clanMapService,
                           MapZoneStyleRegistry styleRegistry) {
        this.clans = clans;
        this.mapHandler = mapHandler;
        this.terrainService = terrainService;
        this.claimIndex = claimIndex;
        this.clanMapService = clanMapService;
        this.styleRegistry = styleRegistry;

        UtilServer.runTaskTimer(clans, this::tick, 1L, 1L);
    }

    public void shutdown() {
        workers.shutdown();
        try {
            if (!workers.awaitTermination(2, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            workers.shutdownNow();
        }
    }

    /**
     * Main-thread pass: decides who needs a new frame and captures everything the build needs, so the worker never
     * touches a live Bukkit object.
     */
    private void tick() {
        if (!mapHandler.isEnabled()) {
            return;
        }

        final boolean fill = Bukkit.getCurrentTick() % FILL_INTERVAL_TICKS == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) {
                continue;
            }

            final MapSettings settings = mapHandler.getOrCreateMapSettings(player);
            if (settings.isBuilding()) {
                continue;
            }

            final World world = player.getWorld();
            final int centerX = player.getLocation().getBlockX();
            final int centerZ = player.getLocation().getBlockZ();

            // Reads any loaded chunk in view that has never been sampled, so an un-regenerated world fills in as
            // players move. Bounded per call, each chunk read once ever, and only on every fourth tick.
            if (fill) {
                terrainService.fillVisible(world, centerX, centerZ, settings.getScale().getValue());
            }

            if (!needsFrame(settings, world, centerX, centerZ)) {
                continue;
            }

            final MapTerrainCache cache = terrainService.cache(world);
            if (cache == null) {
                continue;
            }

            settings.setBuilding(true);
            final int scaleExponent = settings.getScale().getExponent();
            final int terrainGeneration = terrainService.getGeneration(world.getName());
            final int claimGeneration = claimIndex.getGeneration();
            final Long2ObjectMap<MapColor> colours = clanMapService.claimColours(player);

            workers.execute(() -> {
                try {
                    final byte[] frame = build(cache, world.getName(), centerX, centerZ, scaleExponent,
                            terrainGeneration, colours);
                    settings.setMapX(centerX);
                    settings.setMapZ(centerZ);
                    settings.setFrameWorld(world.getName());
                    settings.setTerrainGeneration(terrainGeneration);
                    settings.setClaimGeneration(claimGeneration);
                    settings.setFrame(frame);
                    settings.setFrameDirty(true);
                    // Cleared here rather than at draw time, so a viewer who is not currently rendering does not keep
                    // re-requesting the same frame.
                    settings.setForceRedraw(false);
                } finally {
                    settings.setBuilding(false);
                }
            });
        }
    }

    private boolean needsFrame(MapSettings settings, World world, int centerX, int centerZ) {
        if (settings.isForceRedraw() || settings.getFrame() == null) {
            return true;
        }
        if (!world.getName().equals(settings.getFrameWorld())) {
            return true;
        }
        if (settings.getTerrainGeneration() != terrainService.getGeneration(world.getName())
                || settings.getClaimGeneration() != claimIndex.getGeneration()) {
            return true;
        }
        // Only a move that changes which columns are sampled changes the picture.
        final int scale = settings.getScale().getValue();
        return Math.floorDiv(centerX, scale) != Math.floorDiv(settings.getMapX(), scale)
                || Math.floorDiv(centerZ, scale) != Math.floorDiv(settings.getMapZ(), scale);
    }

    // <editor-fold desc="Composition">

    private byte[] build(MapTerrainCache cache, String worldName, int centerX, int centerZ, int level,
                         int generation, Long2ObjectMap<MapColor> colours) {
        final int scale = 1 << level;
        // floorDiv, not '/', so the view stays centred on the player west and north of the origin.
        final int gridX = Math.floorDiv(centerX, scale);
        final int gridZ = Math.floorDiv(centerZ, scale);

        final TileKey key = new TileKey(worldName, gridX, gridZ, level, generation);
        final byte[] terrain = tiles.get(key, ignored -> terrainTile(cache, gridX, gridZ, level));

        final byte[] frame = terrain.clone();
        drawClaims(frame, worldName, centerX, centerZ, scale, colours);
        return frame;
    }

    /**
     * Copies a window out of the zoom level's mip and fills whatever it has no data for. Because the mip already holds
     * finished pixels, this is a copy plus a background pass — no per-pixel colour work at all.
     */
    private byte[] terrainTile(MapTerrainCache cache, int gridX, int gridZ, int level) {
        final byte[] colors = new byte[MAP_SIZE * MAP_SIZE];
        final boolean[] known = new boolean[MAP_SIZE * MAP_SIZE];

        final int originX = gridX - MAP_CENTER;
        final int originZ = gridZ - MAP_CENTER;
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                final byte packed = cache.mipAt(level, originX + i, originZ + j);
                if (packed != NO_DATA) {
                    final int idx = i * MAP_SIZE + j;
                    colors[idx] = packed;
                    known[idx] = true;
                }
            }
        }

        fillBackground(colors, known);
        return colors;
    }

    /**
     * Fills every pixel with no terrain data. {@link MapBackground#CASCADE} floods the nearest drawn colour outward so
     * the coastline bleeds into the sea; anything still unset takes the flat background colour.
     */
    private void fillBackground(byte[] colors, boolean[] known) {
        if (MapBackground.parse(backgroundModeName, MapBackground.CASCADE) == MapBackground.CASCADE) {
            final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
            for (int idx = 0; idx < colors.length; idx++) {
                if (known[idx]) {
                    queue.enqueue(idx);
                }
            }
            while (!queue.isEmpty()) {
                final int idx = queue.dequeueInt();
                final int ci = idx / MAP_SIZE;
                final int cj = idx % MAP_SIZE;
                final byte color = colors[idx];
                spread(colors, known, queue, ci - 1, cj, color);
                spread(colors, known, queue, ci + 1, cj, color);
                spread(colors, known, queue, ci, cj - 1, color);
                spread(colors, known, queue, ci, cj + 1, color);
            }
        }

        final byte background = backgroundColorId();
        for (int idx = 0; idx < colors.length; idx++) {
            if (!known[idx]) {
                colors[idx] = background;
            }
        }
    }

    private void spread(byte[] colors, boolean[] known, IntArrayFIFOQueue queue, int ci, int cj, byte color) {
        if (ci < 0 || ci >= MAP_SIZE || cj < 0 || cj >= MAP_SIZE) {
            return;
        }
        final int idx = ci * MAP_SIZE + cj;
        if (known[idx]) {
            return;
        }
        known[idx] = true;
        colors[idx] = color;
        queue.enqueue(idx);
    }

    @SuppressWarnings("deprecation")
    private byte backgroundColorId() {
        if (unknownColorId == null) {
            Color color;
            try {
                color = Color.decode(unknownColorHex.startsWith("#") ? unknownColorHex : "#" + unknownColorHex);
            } catch (NumberFormatException exception) {
                color = new Color(58, 106, 154);
            }
            unknownColorId = MapPalette.matchColor(color);
        }
        return unknownColorId;
    }

    // </editor-fold>

    // <editor-fold desc="Clan territory overlay">

    private void drawClaims(byte[] frame, String worldName, int centerX, int centerZ, int scale,
                            Long2ObjectMap<MapColor> colours) {
        final Long2ObjectMap<ClaimEntry> claims = claimIndex.claims(worldName);
        if (claims.isEmpty()) {
            return;
        }

        final MapFill fill = styleRegistry.forTag(CLAN_TERRITORY_TAG)
                .map(MapZoneStyle::getFill)
                .orElse(MapFill.of(MapFillStyle.BORDER));

        final int halfSpan = MAP_CENTER * scale;
        final int minChunkX = (centerX - halfSpan) >> 4;
        final int maxChunkX = (centerX + halfSpan) >> 4;
        final int minChunkZ = (centerZ - halfSpan) >> 4;
        final int maxChunkZ = (centerZ + halfSpan) >> 4;
        final long visibleChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);

        // Whichever is smaller: probing the visible window, or walking every claim in the world.
        if (visibleChunks <= claims.size()) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    final ClaimEntry entry = claims.get(ClanClaimIndex.key(chunkX, chunkZ));
                    if (entry != null) {
                        drawClaim(frame, chunkX, chunkZ, entry, centerX, centerZ, scale, fill, colours);
                    }
                }
            }
            return;
        }

        for (Long2ObjectMap.Entry<ClaimEntry> claim : Long2ObjectMaps.fastIterable(claims)) {
            final int chunkX = ClanClaimIndex.chunkX(claim.getLongKey());
            final int chunkZ = ClanClaimIndex.chunkZ(claim.getLongKey());
            if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ) {
                continue;
            }
            drawClaim(frame, chunkX, chunkZ, claim.getValue(), centerX, centerZ, scale, fill, colours);
        }
    }

    private void drawClaim(byte[] frame, int chunkX, int chunkZ, ClaimEntry entry, int centerX, int centerZ,
                           int scale, MapFill fill, Long2ObjectMap<MapColor> colours) {
        final MapColor colour = colours.getOrDefault(entry.getClanId(), MapColor.GOLD);
        final byte packed = colour.getPackedId(MapColor.Brightness.NORMAL);

        // Converted through the sample grid, exactly as the terrain layer is, so a claim outline lands on the pixels of
        // the chunk it belongs to. Dividing the difference instead would drift by a pixel off multiples of the scale.
        final int pixelX = Math.floorDiv(chunkX << 4, scale) - Math.floorDiv(centerX, scale) + MAP_CENTER;
        final int pixelZ = Math.floorDiv(chunkZ << 4, scale) - Math.floorDiv(centerZ, scale) + MAP_CENTER;
        final int cellSize = Math.max(1, (int) Math.ceil(CHUNK_WIDTH / (double) scale));
        // A pattern needs room. Once a chunk is one or two pixels wide, every pattern lands on every pixel and a block
        // of claims reads as one solid slab, so patterns give way to the outline; an explicit FILL still fills.
        final MapFill effective = cellSize <= 2 && !fill.has(MapFillStyle.FILL) && !fill.needsEdges()
                ? MapFill.of(MapFillStyle.BORDER)
                : fill;

        for (int cx = 0; cx < cellSize; cx++) {
            for (int cz = 0; cz < cellSize; cz++) {
                final int x = pixelX + cx;
                final int z = pixelZ + cz;
                if (x < 0 || x >= MAP_SIZE || z < 0 || z >= MAP_SIZE) {
                    continue;
                }
                if (paintsCellPixel(effective, cx, cz, cellSize, entry)) {
                    frame[x * MAP_SIZE + z] = packed;
                }
            }
        }
    }

    /**
     * Whether a pixel inside a chunk cell is painted, by any style in the fill. Patterns are read in cell-local
     * coordinates so each claim carries its own mark, and {@link MapFillStyle#BORDER} / {@link MapFillStyle#CORNERS} use
     * the claim's exposed edges, which is what keeps a block of claims from reading as one solid slab.
     */
    private boolean paintsCellPixel(MapFill fill, int cx, int cz, int cellSize, ClaimEntry entry) {
        for (MapFillStyle style : fill.getStyles()) {
            if (paintsCellPixel(style, cx, cz, cellSize, entry)) {
                return true;
            }
        }
        return false;
    }

    private boolean paintsCellPixel(MapFillStyle style, int cx, int cz, int cellSize, ClaimEntry entry) {
        return switch (style) {
            case FILL -> true;
            case CHECKER -> ((cx + cz) & 1) == 0;
            case DIAGONAL -> cx == cz;
            case DOTS -> cx == cellSize / 2 && cz == cellSize / 2;
            case CORNERS -> isCorner(cx, cz, cellSize) && isBorder(cx, cz, cellSize, entry);
            case BORDER -> isBorder(cx, cz, cellSize, entry);
        };
    }

    private boolean isCorner(int cx, int cz, int cellSize) {
        return (cx == 0 || cx == cellSize - 1) && (cz == 0 || cz == cellSize - 1);
    }

    private boolean isBorder(int cx, int cz, int cellSize, ClaimEntry entry) {
        return (cx == 0 && !entry.owns(BlockFace.WEST))
                || (cx == cellSize - 1 && !entry.owns(BlockFace.EAST))
                || (cz == 0 && !entry.owns(BlockFace.NORTH))
                || (cz == cellSize - 1 && !entry.owns(BlockFace.SOUTH));
    }

    // </editor-fold>

    @Value
    private static class TileKey {
        String world;
        int gridX;
        int gridZ;
        int level;
        int generation;
    }
}
