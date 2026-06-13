package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.zone.ClanZones;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Manages the periodic respawning of trees in wilderness chunks.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Never force-load unloaded chunks – only operate on already-loaded chunks.</li>
 *   <li>Never touch any chunk owned by a clan (including admin clans).</li>
 *   <li>Process one chunk per second to keep main-thread impact negligible.</li>
 *   <li>Use a chunk-coordinate-seeded {@link Random} so that the same chunk always
 *       gets the same tree type/position (deterministic, seed-based behavior).</li>
 * </ul>
 */
@CustomLog
@Singleton
public class TreeRespawnManager {

    private static final int PLAYER_TERRITORY_BUFFER_BLOCKS = 3;

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final ClanManager clanManager;
    private final ZoneManager zoneManager;

    // -----------------------------------------------------------------------
    // Config
    // -----------------------------------------------------------------------

    /**
     * Master on/off switch.  Set to {@code false} to disable the entire system.
     */
    @Getter
    @Config(path = "trees.respawn.enabled", defaultValue = "true")
    @Inject
    private boolean enabled;

    /**
     * Minimum number of log-column samples (out of 16 sampled positions) that must
     * be present in a chunk before the system considers the chunk "forested enough"
     * and skips it.  A lower value means only very barren chunks are targeted.
     *
     * <p>The chunk is sampled at a 4×4 grid (16 positions total).  Each position
     * contributes +1 if any log or wood block exists within 10 blocks above the
     * surface at that x/z.  Typical untouched-forest chunks score 6–12.
     */
    @Config(path = "trees.respawn.min-log-count", defaultValue = "2")
    @Inject
    private int minLogCount;

    /**
     * Maximum number of chunks held in the pending queue at any one time.
     * Prevents unbounded memory growth when many chunks are loaded.
     */
    @Config(path = "trees.respawn.max-queue-size", defaultValue = "200")
    @Inject
    private int maxQueueSize;

    // -----------------------------------------------------------------------
    // Queue state
    // -----------------------------------------------------------------------

    /**
     * Pairs a pre-computed chunk key with a weak reference to the chunk so that
     * (a) the queue never prevents an unloaded chunk from being garbage-collected,
     * and (b) the key can still be removed from {@link #queuedChunkKeys} even after
     * the referent has been collected.
     */
    private record ChunkRef(long key, WeakReference<Chunk> ref) {
    }

    /**
     * Chunks waiting to be evaluated for tree respawn.
     */
    private final Queue<ChunkRef> pendingChunks = new ArrayDeque<>();

    /**
     * Tracks the keys of chunks currently in {@link #pendingChunks} for O(1) dedup.
     */
    private final Set<Long> queuedChunkKeys = new HashSet<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    @Inject
    public TreeRespawnManager(ClanManager clanManager, ZoneManager zoneManager) {
        this.clanManager = clanManager;
        this.zoneManager = zoneManager;
    }


    /**
     * Returns the number of chunks currently waiting in the processing queue.
     */
    public int getQueueSize() {
        return pendingChunks.size();
    }

    /**
     * Attempts to add {@code chunk} to the processing queue.
     *
     * <p>The chunk is silently rejected if:
     * <ul>
     *   <li>the system is disabled,</li>
     *   <li>the queue is full,</li>
     *   <li>the chunk is already queued, or</li>
     *   <li>the chunk belongs to a clan.</li>
     * </ul>
     *
     * @param chunk the chunk to evaluate
     */
    public void queueChunk(Chunk chunk) {
        if (!enabled) return;
        if (pendingChunks.size() >= maxQueueSize) return;

        long key = chunkKey(chunk);
        if (queuedChunkKeys.contains(key)) return;
        if (clanManager.getClanByChunk(chunk).isPresent()) return;

        queuedChunkKeys.add(key);
        pendingChunks.offer(new ChunkRef(key, new WeakReference<>(chunk)));
    }

    /**
     * Dequeues one chunk and processes it.
     * <b>Must be called on the main server thread.</b>
     */
    public void processNext() {
        if (!enabled) return;

        ChunkRef entry = pendingChunks.poll();
        if (entry == null) return;

        // Always remove the key first so the slot is freed regardless of GC state
        queuedChunkKeys.remove(entry.key());

        Chunk chunk = entry.ref().get();
        if (chunk == null) return; // chunk was unloaded and GC'd while queued

        processChunk(chunk);
    }

    // -----------------------------------------------------------------------
    // Internal logic
    // -----------------------------------------------------------------------

    /**
     * Evaluates one chunk and, if it is deforested enough and has a valid spot,
     * spawns one tree.
     */
    private void processChunk(Chunk chunk) {
        World world = chunk.getWorld();

        // Only operate on the main overworld
        if (!world.getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) return;

        // Confirm the chunk is still loaded (it may have unloaded while queued)
        if (!world.isChunkLoaded(chunk.getX(), chunk.getZ())) return;

        // Re-check territory claim (claim may have changed while chunk was queued)
        if (clanManager.getClanByChunk(chunk).isPresent()) return;

        // Count existing log-columns in the chunk sample
        int logCount = countLogColumnsInChunk(chunk);
        if (logCount >= minLogCount) return; // chunk is forested enough

        // Deterministic RNG seeded by world seed + chunk coordinates
        long seed = world.getSeed() ^ ((long) chunk.getX() << 32) ^ (chunk.getZ() & 0xFFFFFFFFL);
        Random random = new Random(seed);

        // Find a valid planting spot
        Location spot = findValidTreeSpot(chunk, random);
        if (spot == null) return; // no valid spot exists (terrain altered, etc.)

        if(isWithinPlayerTerritoryBuffer(spot)) return;

        // Determine tree type from biome at that spot
        Biome biome = world.getBiome(spot.getBlockX(), spot.getBlockY(), spot.getBlockZ());
        TreeType treeType = getTreeTypeForBiome(biome, random);

        // generateTree(Location, Random, TreeType) is the non-deprecated overload in Paper 1.21.6+
        boolean spawned = world.generateTree(spot, random, treeType);
        if (spawned) {
            log.info("TreeRespawn: spawned {} at {},{},{} (chunk {},{}, logSample={})",
                    treeType,
                    spot.getBlockX(), spot.getBlockY(), spot.getBlockZ(),
                    chunk.getX(), chunk.getZ(),
                    logCount).submit();
        }
    }

    /**
     * Samples a 4×4 grid across the chunk (16 positions, every 4 blocks in X and Z)
     * and counts how many sampled columns contain at least one log or wood block
     * within 10 blocks above the surface.
     *
     * <p>This is intentionally cheap: we only read 16×10 = 160 block types maximum.
     */
    private int countLogColumnsInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int count = 0;

        for (int dx = 0; dx < 16; dx += 4) {
            for (int dz = 0; dz < 16; dz += 4) {
                int wx = baseX + dx;
                int wz = baseZ + dz;
                int surfaceY = world.getHighestBlockYAt(wx, wz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                for (int dy = 0; dy <= 10; dy++) {
                    Block block = world.getBlockAt(wx, surfaceY + dy, wz);
                    if (isLog(block.getType())) {
                        count++;
                        break; // only count one per column
                    }
                }
            }
        }

        return count;
    }

    /**
     * Tries up to 8 random positions within the chunk (using the deterministic
     * {@code random}) and returns the first location that is:
     * <ul>
     *   <li>a natural tree-compatible surface block (grass, dirt, podzol, etc.), and</li>
     *   <li>has full sky light (15) at the block immediately above it.</li>
     * </ul>
     *
     * @return a planting {@link Location}, or {@code null} if no valid spot was found
     * after 8 attempts (e.g., the terrain has been built over or is fully shaded)
     */
    private Location findValidTreeSpot(Chunk chunk, Random random) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        for (int attempt = 0; attempt < 8; attempt++) {
            int wx = baseX + random.nextInt(16);
            int wz = baseZ + random.nextInt(16);

            int surfaceY = world.getHighestBlockYAt(wx, wz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block surface = world.getBlockAt(wx, surfaceY, wz);
            Block above = world.getBlockAt(wx, surfaceY + 1, wz);

            if (!isValidTreeBase(surface.getType())) continue;
            if (!above.getType().isAir()) continue;
            if (above.getLightFromSky() < 15) continue;

            // Return the block *on top of* the surface block (where the sapling would be)
            return new Location(world, wx, surfaceY + 1, wz);
        }

        return null;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Encodes chunk coordinates into a single {@code long} for fast dedup.
     */
    private static long chunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
    }

    private static boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD");
    }

    private static boolean isValidTreeBase(Material material) {
        return switch (material) {
            case GRASS_BLOCK, DIRT, COARSE_DIRT, PODZOL, ROOTED_DIRT, MYCELIUM -> true;
            default -> false;
        };
    }

    /**
     * Prevents trees from respawning too close to non-admin clan claims, while still
     * allowing admin-owned map regions to behave as before.
     */
    private boolean isWithinPlayerTerritoryBuffer(Location location) {

        for (int offsetX = -PLAYER_TERRITORY_BUFFER_BLOCKS; offsetX <= PLAYER_TERRITORY_BUFFER_BLOCKS; offsetX++) {
            for (int offsetZ = -PLAYER_TERRITORY_BUFFER_BLOCKS; offsetZ <= PLAYER_TERRITORY_BUFFER_BLOCKS; offsetZ++) {
                if (offsetX == 0 && offsetZ == 0) continue;

                Location bufferedLocation = location.clone().add(offsetX, 0, offsetZ);
                final Zone zoneAt = zoneManager.getZoneAt(bufferedLocation);
                if (zoneAt != null && zoneAt.hasTag(ClanZones.TERRITORY)) {
                    return true; // if it's a clan territory return true
                }
            }
        }

        return false;
    }

    /**
     * Maps a {@link Biome} to a {@link TreeType}.
     *
     * <p>{@code Biome} is a registry interface in Paper 1.21+ (not an enum), so a
     * switch expression cannot be used.  Instead, every comparison is written as
     * {@code biome == Biome.CONSTANT}, which gives a <b>compile-time error</b> if
     * Paper ever renames or removes a constant — the same "hard-fail" guarantee as
     * an exhaustive enum switch.
     */
    private static TreeType getTreeTypeForBiome(Biome biome, Random random) {
        // --- Birch ---
        if (biome == Biome.BIRCH_FOREST || biome == Biome.OLD_GROWTH_BIRCH_FOREST)
            return random.nextBoolean() ? TreeType.BIRCH : TreeType.TALL_BIRCH;

        // --- Dark oak ---
        if (biome == Biome.DARK_FOREST)
            return TreeType.DARK_OAK;

        // --- Jungle ---
        if (biome == Biome.JUNGLE || biome == Biome.BAMBOO_JUNGLE)
            return random.nextInt(3) == 0 ? TreeType.JUNGLE : TreeType.SMALL_JUNGLE;
        if (biome == Biome.SPARSE_JUNGLE)
            return TreeType.SMALL_JUNGLE;

        // --- Mangrove ---
        if (biome == Biome.MANGROVE_SWAMP)
            return TreeType.MANGROVE;

        // --- Cherry ---
        if (biome == Biome.CHERRY_GROVE)
            return TreeType.CHERRY;

        // --- Pale oak (added 1.21.4) ---
        if (biome == Biome.PALE_GARDEN)
            return TreeType.PALE_OAK;

        // --- Spruce / taiga ---
        if (biome == Biome.OLD_GROWTH_PINE_TAIGA || biome == Biome.OLD_GROWTH_SPRUCE_TAIGA)
            return random.nextBoolean() ? TreeType.MEGA_REDWOOD : TreeType.REDWOOD;
        if (biome == Biome.TAIGA || biome == Biome.SNOWY_TAIGA
                || biome == Biome.GROVE || biome == Biome.SNOWY_SLOPES)
            return TreeType.REDWOOD;

        // --- Acacia ---
        if (biome == Biome.SAVANNA || biome == Biome.SAVANNA_PLATEAU || biome == Biome.WINDSWEPT_SAVANNA)
            return TreeType.ACACIA;

        // --- Swamp oak ---
        if (biome == Biome.SWAMP)
            return TreeType.SWAMP;

        // --- Forest oak / birch mix ---
        if (biome == Biome.FOREST || biome == Biome.FLOWER_FOREST || biome == Biome.WINDSWEPT_FOREST)
            return random.nextBoolean() ? TreeType.TREE : TreeType.BIRCH;

        // --- Open overworld biomes (lone oak) ---
        if (biome == Biome.PLAINS || biome == Biome.SUNFLOWER_PLAINS || biome == Biome.MEADOW
                || biome == Biome.WINDSWEPT_HILLS || biome == Biome.WINDSWEPT_GRAVELLY_HILLS)
            return TreeType.TREE;

        // --- All remaining biomes (desert, ocean, nether, end, caves, etc.).
        //     The spot-validity check (grass/dirt + sky-light 15) prevents trees
        //     from actually spawning in these biomes anyway. ---
        return random.nextInt(4) == 0 ? TreeType.BIG_TREE : TreeType.TREE;
    }
}

