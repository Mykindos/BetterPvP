package me.mykindos.betterpvp.clans.clans.map.terrain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.map.MapColorBlender;
import me.mykindos.betterpvp.clans.clans.map.MapZoneStyleRegistry;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Owns the terrain layer for every mapped world: the flat {@link MapTerrainCache}s, the sampling that fills them, the
 * mipmaps drawn from, and their on-disk form.
 * <p>
 * Every mutation of a cache is funnelled onto one {@link #worker} thread, so the arrays need no locking and the main
 * thread never pays for a rebuild. Block changes are coalesced through a long queue whose de-duplication is O(1), so
 * a lava flow or a TNT volley costs work proportional to the columns actually touched.
 */
@CustomLog
@BPvPListener
@Singleton
public class MapTerrainService implements Listener {

    private static final int MAGIC = 0x42_50_4D_50; // "BPMP"
    private static final int VERSION = 2;
    private static final String DATA_FILE = "bpvp_map.dat";

    private static final int REGEN_BATCH = 32;
    /** Block edits are drained every tick, so a change reaches the map within a frame or two of happening. */
    private static final int QUEUE_INTERVAL_TICKS = 1;
    private static final long SAVE_INTERVAL_TICKS = 6000;
    private static final long ZONE_POLL_TICKS = 200;
    private static final int MAP_SIZE = 128;
    /** Chunks examined per viewer per opportunistic fill, bounding the cost at maximum zoom. */
    private static final int FILL_CHUNK_BUDGET = 24;
    /** How far below the surface a resample will descend looking for a block that paints the map. */
    private static final int MAX_COLOUR_DESCENT = 64;

    private final Clans clans;
    private final ZoneManager zoneManager;
    private final MapZoneStyleRegistry styleRegistry;
    private final MapColorBlender blender;

    @Inject
    @Config(path = "clans.map.maxMapDistance", defaultValue = "645")
    private int maxDistance;

    @Inject
    @Config(path = "clans.map.maxProcess", defaultValue = "64")
    private int maxProcess;

    private final Map<String, MapTerrainCache> caches = new ConcurrentHashMap<>();
    private final Map<String, ZoneStyleSnapshot> zoneSnapshots = new ConcurrentHashMap<>();

    /** Pending column resamples, keyed by world. Both structures are touched only on the main thread. */
    private final Map<String, LongOpenHashSet> pending = new ConcurrentHashMap<>();
    private final Map<String, LongArrayFIFOQueue> queues = new ConcurrentHashMap<>();
    /** Chunks already read into a cache, so the opportunistic fill never samples the same chunk twice. */
    private final Map<String, LongOpenHashSet> sampledChunks = new ConcurrentHashMap<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        final Thread thread = new Thread(runnable, "BPvP-Map-Terrain");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean regenerating = new AtomicBoolean();
    /**
     * Bumped whenever a world's mips change, so frame caches know their contents are stale. Kept per world so a block
     * change on one island does not invalidate every other world's cached tiles.
     */
    private final Map<String, AtomicInteger> generations = new ConcurrentHashMap<>();
    /** Hash of the styled zone set, so {@link #pollZones()} can notice zones appearing or being reloaded. */
    private int zoneSignature;

    @Inject
    public MapTerrainService(Clans clans, ZoneManager zoneManager, MapZoneStyleRegistry styleRegistry,
                             MapColorBlender blender) {
        this.clans = clans;
        this.zoneManager = zoneManager;
        this.styleRegistry = styleRegistry;
        this.blender = blender;

        UtilServer.runTaskTimer(clans, this::drainQueues, QUEUE_INTERVAL_TICKS, QUEUE_INTERVAL_TICKS);
        UtilServer.runTaskTimer(clans, this::saveAll, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
        UtilServer.runTaskTimer(clans, this::pollZones, ZONE_POLL_TICKS, ZONE_POLL_TICKS);
        // Tints are baked into the mips, so a style or zone change has to re-capture geometry and rebuild them.
        styleRegistry.onInvalidate(this::invalidateAllZones);
    }

    /**
     * Watches for the styled zone set changing and rebuilds tints when it does.
     * <p>
     * Zones are installed by the world-content pipeline as worlds load and reload, which can happen long after a
     * terrain cache is first prepared, and no event announces it. Since tints are baked into the mips rather than
     * resolved per pixel at draw time, a cache captured before its world's zones existed would stay untinted forever.
     */
    private void pollZones() {
        if (caches.isEmpty()) {
            return;
        }

        int signature = 1;
        for (Zone zone : zoneManager.getAllZones()) {
            if (styleRegistry.resolve(List.of(zone)).isPresent()) {
                signature = signature * 31 + zone.getKey().hashCode();
            }
        }
        if (signature == zoneSignature) {
            return;
        }
        zoneSignature = signature;
        invalidateAllZones();
    }

    /**
     * Marks every chunk inside the mapped box as already read, so the opportunistic fill skips a complete world.
     */
    private void markAllSampled(String world, MapTerrainCache cache) {
        final LongOpenHashSet sampled = sampledChunks.computeIfAbsent(world, ignored -> new LongOpenHashSet());
        final int radius = cache.getRadius();
        for (int chunkX = -radius >> 4; chunkX <= radius >> 4; chunkX++) {
            for (int chunkZ = -radius >> 4; chunkZ <= radius >> 4; chunkZ++) {
                sampled.add(((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL));
            }
        }
    }

    /**
     * Re-captures zone geometry and rebuilds mips for every mapped world.
     */
    public void invalidateAllZones() {
        for (String name : caches.keySet()) {
            final World world = Bukkit.getWorld(name);
            if (world != null) {
                invalidateZones(world);
            }
        }
    }

    /**
     * @param world a world name
     * @return a counter bumped every time that world's mips change
     */
    public int getGeneration(String world) {
        final AtomicInteger counter = generations.get(world);
        return counter == null ? 0 : counter.get();
    }

    private void bump(String world) {
        generations.computeIfAbsent(world, ignored -> new AtomicInteger()).incrementAndGet();
    }

    // <editor-fold desc="Cache access">

    /**
     * @return the terrain cache for a world, loading it from disk on first use, or {@code null} while that load is
     * still in flight
     */
    public @Nullable MapTerrainCache cache(@NotNull World world) {
        final MapTerrainCache existing = caches.get(world.getName());
        if (existing != null) {
            return existing;
        }
        prepare(world);
        return null;
    }

    /**
     * Ensures a world has a cache, reading its saved terrain and rebuilding mips off-thread. Idempotent.
     */
    public void prepare(@NotNull World world) {
        final String name = world.getName();
        if (caches.containsKey(name)) {
            return;
        }
        final MapTerrainCache cache = new MapTerrainCache(name, maxDistance);
        if (caches.putIfAbsent(name, cache) != null) {
            return;
        }

        final ZoneStyleSnapshot snapshot = ZoneStyleSnapshot.capture(world, zoneManager, styleRegistry);
        zoneSnapshots.put(name, snapshot);

        worker.execute(() -> {
            final long start = System.currentTimeMillis();
            final boolean loaded = load(world, cache);
            cache.rebuildAllMips(snapshot, blender);
            bump(name);
            if (loaded) {
                log.info("Loaded map terrain for '{}' in {}", name, UtilTime.getTime(System.currentTimeMillis() - start, 2))
                        .submit();
            }
        });
    }

    /**
     * Re-captures zone geometry and rebuilds every mip for a world. Use after zones or styles change.
     */
    public void invalidateZones(@NotNull World world) {
        final MapTerrainCache cache = caches.get(world.getName());
        if (cache == null) {
            return;
        }
        final ZoneStyleSnapshot snapshot = ZoneStyleSnapshot.capture(world, zoneManager, styleRegistry);
        zoneSnapshots.put(world.getName(), snapshot);
        worker.execute(() -> {
            cache.invalidateZoneStyles();
            cache.rebuildAllMips(snapshot, blender);
            bump(world.getName());
        });
    }

    // </editor-fold>

    // <editor-fold desc="Opportunistic fill">

    /**
     * Samples any chunk in a viewer's window that is loaded but not yet in the cache, so a world that has never been
     * regenerated still fills in as players move.
     * <p>
     * Only already-loaded chunks are read, since forcing a load here would block the server on terrain generation. Each
     * chunk is sampled once ever, and no more than {@link #FILL_CHUNK_BUDGET} are examined per call, so a viewer at
     * maximum zoom cannot turn this into a sweep of a quarter-million chunks.
     */
    public void fillVisible(@NotNull World world, int centerX, int centerZ, int scale) {
        final MapTerrainCache cache = caches.get(world.getName());
        final ZoneStyleSnapshot snapshot = zoneSnapshots.get(world.getName());
        if (cache == null || snapshot == null || regenerating.get()) {
            return;
        }

        final LongOpenHashSet sampled = sampledChunks.computeIfAbsent(world.getName(),
                ignored -> new LongOpenHashSet());
        final int halfSpan = (MAP_SIZE / 2) * scale;
        final int minChunkX = (centerX - halfSpan) >> 4;
        final int maxChunkX = (centerX + halfSpan) >> 4;
        final int minChunkZ = (centerZ - halfSpan) >> 4;
        final int maxChunkZ = (centerZ + halfSpan) >> 4;

        int examined = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX && examined < FILL_CHUNK_BUDGET; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ && examined < FILL_CHUNK_BUDGET; chunkZ++) {
                final long key = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                if (sampled.contains(key) || !cache.inBounds(chunkX << 4, chunkZ << 4)) {
                    continue;
                }
                examined++;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                sampled.add(key);
                final int baseX = chunkX << 4;
                final int baseZ = chunkZ << 4;
                // Terrain restored from disk is already here; the sampled set is only an in-memory shortcut, so the
                // cache itself has to be the authority or a reload would re-read the whole world.
                if (cache.hasColumn(baseX + 8, baseZ + 8)) {
                    continue;
                }

                // The first flag is includeMaxBlockY: without it the snapshot carries no height map and every column
                // reads as empty.
                final ChunkSnapshot chunkSnapshot = world.getChunkAt(chunkX, chunkZ)
                        .getChunkSnapshot(true, false, false);
                final int minY = world.getMinHeight();
                final int maxY = world.getMaxHeight();
                worker.execute(() -> {
                    readChunk(cache, chunkSnapshot, baseX, baseZ, minY, maxY);
                    cache.refreshArea(baseX, baseZ, baseX + 15, baseZ + 15, snapshot, blender);
                    bump(world.getName());
                });
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="Live block changes">

    private void queue(Block block) {
        final String name = block.getWorld().getName();
        if (!caches.containsKey(name)) {
            return;
        }
        final long key = ((long) block.getX() << 32) | (block.getZ() & 0xFFFFFFFFL);
        // O(1) de-dupe, so a burst of block changes over the same columns collapses instead of compounding.
        if (pending.computeIfAbsent(name, ignored -> new LongOpenHashSet()).add(key)) {
            queues.computeIfAbsent(name, ignored -> new LongArrayFIFOQueue()).enqueue(key);
        }
    }

    private void drainQueues() {
        for (Map.Entry<String, LongArrayFIFOQueue> entry : queues.entrySet()) {
            final String worldName = entry.getKey();
            final World world = Bukkit.getWorld(worldName);
            final MapTerrainCache cache = caches.get(worldName);
            final ZoneStyleSnapshot snapshot = zoneSnapshots.get(worldName);
            if (world == null || cache == null || snapshot == null) {
                continue;
            }

            final LongArrayFIFOQueue queue = entry.getValue();
            final LongOpenHashSet seen = pending.get(worldName);
            final List<long[]> sampled = new ArrayList<>();

            for (int processed = 0; processed < maxProcess && !queue.isEmpty(); processed++) {
                final long key = queue.dequeueLong();
                seen.remove(key);

                final int x = (int) (key >> 32);
                final int z = (int) key;
                if (!cache.inBounds(x, z) || !world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                sampled.add(sampleColumn(world, x, z));
            }

            if (!sampled.isEmpty()) {
                worker.execute(() -> {
                    for (long[] column : sampled) {
                        cache.setColumn((int) column[0], (int) column[1], (int) column[2], (short) column[3]);
                    }
                    for (long[] column : sampled) {
                        cache.refreshColumn((int) column[0], (int) column[1], snapshot, blender);
                    }
                    bump(worldName);
                });
            }
        }
    }

    /**
     * Reads one column on the main thread, descending past blocks that do not paint the map.
     *
     * @return {@code {x, z, colourId, height}}
     */
    private long[] sampleColumn(World world, int x, int z) {
        Block block = world.getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE);
        int colour = UtilMapMaterial.getColorId(block.getType());
        for (int steps = 0; colour == MapColor.NONE.id && steps < MAX_COLOUR_DESCENT
                && block.getY() > world.getMinHeight(); steps++) {
            block = world.getBlockAt(x, block.getY() - 1, z);
            colour = UtilMapMaterial.getColorId(block.getType());
        }
        return new long[]{x, z, colour, block.getY()};
    }

    // </editor-fold>

    // <editor-fold desc="Regeneration">

    /**
     * Rebuilds a world's whole displayable area by streaming chunks in asynchronously and sampling their snapshots off
     * the main thread. Chunks are generated if missing, so keep the world border sized to the playable area.
     *
     * @param world     the world to regenerate
     * @param initiator the player to report progress to, or {@code null}
     */
    public void regenerate(@NotNull World world, @Nullable Player initiator) {
        if (!regenerating.compareAndSet(false, true)) {
            if (initiator != null) {
                UtilMessage.message(initiator, "clans.prefix", "A map regeneration is already running.");
            }
            return;
        }

        prepare(world);
        final MapTerrainCache cache = caches.get(world.getName());
        final ZoneStyleSnapshot snapshot = ZoneStyleSnapshot.capture(world, zoneManager, styleRegistry);
        zoneSnapshots.put(world.getName(), snapshot);

        int minX = -maxDistance;
        int maxX = maxDistance;
        int minZ = -maxDistance;
        int maxZ = maxDistance;
        final WorldBorder border = world.getWorldBorder();
        final Location center = border.getCenter();
        final double half = border.getSize() / 2.0;
        minX = Math.max(minX, (int) Math.floor(center.getX() - half));
        maxX = Math.min(maxX, (int) Math.ceil(center.getX() + half));
        minZ = Math.max(minZ, (int) Math.floor(center.getZ() - half));
        maxZ = Math.min(maxZ, (int) Math.ceil(center.getZ() + half));

        final ArrayDeque<int[]> chunks = new ArrayDeque<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                chunks.add(new int[]{chunkX, chunkZ});
            }
        }

        if (initiator != null) {
            UtilMessage.message(initiator, "clans.prefix",
                    "Regenerating map (" + chunks.size() + " chunks) in '" + world.getName() + "'…");
        }
        regenerateBatch(world, cache, snapshot, chunks, initiator);
    }

    private void regenerateBatch(World world, MapTerrainCache cache, ZoneStyleSnapshot snapshot,
                                 ArrayDeque<int[]> chunks, Player initiator) {
        if (chunks.isEmpty()) {
            worker.execute(() -> {
                cache.rebuildAllMips(snapshot, blender);
                bump(world.getName());
                regenerating.set(false);
                save(world, cache);
                UtilServer.runTask(clans, () -> {
                    markAllSampled(world.getName(), cache);
                    if (initiator != null && initiator.isOnline()) {
                        UtilMessage.message(initiator, "clans.prefix", "Map regeneration complete.");
                    }
                });
            });
            return;
        }

        final int batch = Math.min(REGEN_BATCH, chunks.size());
        final List<CompletableFuture<Void>> futures = new ArrayList<>(batch);
        for (int i = 0; i < batch; i++) {
            final int[] coord = chunks.poll();
            futures.add(world.getChunkAtAsync(coord[0], coord[1]).thenAccept(chunk -> {
                // Snapshot on the main thread (where the callback lands), then read all 256 columns off it. The first
                // flag is includeMaxBlockY: without it the snapshot carries no height map.
                final ChunkSnapshot snapshotOfChunk = chunk.getChunkSnapshot(true, false, false);
                final int baseX = chunk.getX() << 4;
                final int baseZ = chunk.getZ() << 4;
                final int minY = world.getMinHeight();
                final int maxY = world.getMaxHeight();
                worker.execute(() -> readChunk(cache, snapshotOfChunk, baseX, baseZ, minY, maxY));
            }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRun(() -> UtilServer.runTask(clans,
                        () -> regenerateBatch(world, cache, snapshot, chunks, initiator)));
    }

    /**
     * Reads all 256 columns of a chunk snapshot off the main thread. Bounds are passed in rather than read from the
     * chunk, so this touches nothing live.
     */
    private void readChunk(MapTerrainCache cache, ChunkSnapshot snapshot, int baseX, int baseZ, int minY, int maxY) {
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                final int x = baseX + localX;
                final int z = baseZ + localZ;
                if (!cache.inBounds(x, z)) {
                    continue;
                }

                // Height maps report the first empty slot, so the descent below also absorbs that off-by-one.
                int y = Math.min(snapshot.getHighestBlockYAt(localX, localZ), maxY - 1);
                if (y < minY) {
                    continue;
                }

                int colour = UtilMapMaterial.getColorId(snapshot.getBlockType(localX, y, localZ));
                for (int steps = 0; colour == MapColor.NONE.id && steps < MAX_COLOUR_DESCENT && y > minY; steps++) {
                    y--;
                    final Material material = snapshot.getBlockType(localX, y, localZ);
                    colour = UtilMapMaterial.getColorId(material);
                }
                cache.setColumn(x, z, colour, (short) y);
            }
        }
    }

    // </editor-fold>

    // <editor-fold desc="Persistence">

    /**
     * Clears a world's terrain and deletes its saved copy.
     */
    public void reset(@NotNull World world) {
        final File file = dataFile(world);
        if (file.exists() && !file.delete()) {
            log.error("Failed to delete map data file for '{}'", world.getName()).submit();
        }
        caches.remove(world.getName());
        zoneSnapshots.remove(world.getName());
        pending.remove(world.getName());
        queues.remove(world.getName());
        sampledChunks.remove(world.getName());
        bump(world.getName());
    }

    /**
     * Schedules a save of every mapped world, and releases the cache of any world nobody is in.
     * <p>
     * A cache is a few megabytes of flat arrays per world, and instanced island worlds can exist by the dozen, so an
     * unwatched world hands its memory back and reloads from disk the next time someone opens a map there.
     */
    public void saveAll() {
        for (Map.Entry<String, MapTerrainCache> entry : caches.entrySet()) {
            final String name = entry.getKey();
            final World world = Bukkit.getWorld(name);
            if (world == null) {
                release(name);
                continue;
            }

            final MapTerrainCache cache = entry.getValue();
            final boolean occupied = !world.getPlayers().isEmpty();
            worker.execute(() -> {
                save(world, cache);
                if (!occupied && !regenerating.get()) {
                    UtilServer.runTask(clans, () -> release(name));
                }
            });
        }
    }

    private void release(String world) {
        caches.remove(world);
        zoneSnapshots.remove(world);
        pending.remove(world);
        queues.remove(world);
        sampledChunks.remove(world);
    }

    /**
     * Writes every mapped world on the calling thread and stops the worker. For shutdown, where scheduled tasks and
     * executors no longer run reliably.
     */
    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            worker.shutdownNow();
        }

        for (Map.Entry<String, MapTerrainCache> entry : caches.entrySet()) {
            final World world = Bukkit.getWorld(entry.getKey());
            if (world != null) {
                save(world, entry.getValue());
            }
        }
    }

    private File dataFile(World world) {
        return new File(new File(world.getWorldFolder(), "data"), DATA_FILE);
    }

    /**
     * Terrain is written as two raw blocks, colours then heights, through a deflater. A world's worth of terrain is a
     * couple of megabytes on disk and loads with a single {@code readFully} per block.
     */
    private void save(World world, MapTerrainCache cache) {
        final File file = dataFile(world);
        final File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            log.error("Failed to create map data directory for '{}'", world.getName()).submit();
            return;
        }

        final int columns = cache.columnCount();
        final byte[] colours = new byte[columns];
        System.arraycopy(cache.colorArray(), 0, colours, 0, columns);
        final ByteBuffer heights = ByteBuffer.allocate(columns * 2);
        heights.asShortBuffer().put(cache.heightArray(), 0, columns);

        try (DataOutputStream out = new DataOutputStream(
                new DeflaterOutputStream(new BufferedOutputStream(new FileOutputStream(file))))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(cache.getRadius());
            out.writeInt(columns);
            out.write(colours);
            out.write(heights.array());
        } catch (IOException exception) {
            log.error("Failed to save map data for '{}'", world.getName(), exception).submit();
        }
    }

    private boolean load(World world, MapTerrainCache cache) {
        final File file = dataFile(world);
        if (!file.exists()) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(
                new InflaterInputStream(new BufferedInputStream(new FileInputStream(file))))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                log.warn("Ignoring map data for '{}' with an unrecognised format", world.getName()).submit();
                return false;
            }
            final int radius = in.readInt();
            final int columns = in.readInt();
            if (radius != cache.getRadius() || columns != cache.columnCount()) {
                log.warn("Ignoring map data for '{}': saved radius {} does not match configured {}",
                        world.getName(), radius, cache.getRadius()).submit();
                return false;
            }

            in.readFully(cache.colorArray(), 0, columns);
            final byte[] heights = new byte[columns * 2];
            in.readFully(heights);
            ByteBuffer.wrap(heights).asShortBuffer().get(cache.heightArray(), 0, columns);
            return true;
        } catch (IOException exception) {
            log.error("Failed to load map data for '{}'", world.getName(), exception).submit();
            return false;
        }
    }

    // </editor-fold>

    // <editor-fold desc="Block events">

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        queue(event.getBlock());
        queue(event.getToBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        switch (event.getChangedType()) {
            case LAVA, WATER -> queue(event.getBlock());
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        queue(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        queue(event.getBlock());
    }

    // </editor-fold>
}
