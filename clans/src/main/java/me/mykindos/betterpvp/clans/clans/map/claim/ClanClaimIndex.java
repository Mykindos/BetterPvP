package me.mykindos.betterpvp.clans.clans.map.claim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single, viewer-independent index of every clan claim, keyed by world and packed chunk coordinate.
 * <p>
 * A claim is stored once for the whole server; a viewer contributes only a colour, resolved separately from their
 * relation table. That keeps a join free of map work entirely, and makes a claim edit cost one debounced rebuild
 * proportional to the number of claims rather than to claims times online players.
 */
@Singleton
public class ClanClaimIndex {

    private static final long REBUILD_DEBOUNCE_TICKS = 20L;

    private final Clans clans;
    private final ClanManager clanManager;

    private volatile Map<String, Long2ObjectMap<ClaimEntry>> byWorld = Map.of();
    private volatile int generation;
    private final AtomicBoolean rebuildScheduled = new AtomicBoolean();

    @Inject
    public ClanClaimIndex(Clans clans, ClanManager clanManager) {
        this.clans = clans;
        this.clanManager = clanManager;
    }

    public int getGeneration() {
        return generation;
    }

    /**
     * @param world a world name
     * @return that world's claims, keyed by {@link #key(int, int)}; never null
     */
    public @NotNull Long2ObjectMap<ClaimEntry> claims(@NotNull String world) {
        final Long2ObjectMap<ClaimEntry> claims = byWorld.get(world);
        return claims == null ? Long2ObjectMaps.emptyMap() : claims;
    }

    public static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public static int chunkX(long key) {
        return (int) (key >> 32);
    }

    public static int chunkZ(long key) {
        return (int) key;
    }

    /**
     * Requests a rebuild. Calls within the debounce window collapse into one, so a clan claiming twenty chunks in a row
     * rebuilds once rather than twenty times.
     */
    public void scheduleRebuild() {
        if (!rebuildScheduled.compareAndSet(false, true)) {
            return;
        }
        UtilServer.runTaskLater(clans, () -> {
            rebuildScheduled.set(false);
            rebuild();
        }, REBUILD_DEBOUNCE_TICKS);
    }

    /**
     * Rebuilds the index from the live clan set. Runs on the main thread — it reads clan territory lists — but is
     * O(claims) with no per-player work, so it stays well under a tick.
     */
    public void rebuild() {
        final Map<String, Long2ObjectMap<ClaimEntry>> built = new ConcurrentHashMap<>();

        // First pass: record which chunks each clan owns, so the second pass can work out exposed sides.
        final Map<String, Map<Long, Long>> ownerByWorld = new ConcurrentHashMap<>();
        for (Clan clan : clanManager.getObjects().values()) {
            for (ClanTerritory claim : clan.getTerritory()) {
                final String[] tokens = claim.getChunk().split("/ ");
                if (tokens.length != 3) {
                    continue;
                }
                try {
                    final int chunkX = Integer.parseInt(tokens[1].trim());
                    final int chunkZ = Integer.parseInt(tokens[2].trim());
                    ownerByWorld.computeIfAbsent(tokens[0].trim(), ignored -> new ConcurrentHashMap<>())
                            .put(key(chunkX, chunkZ), clan.getId());
                } catch (NumberFormatException ignored) {
                    // Malformed territory row; skip it rather than failing the whole index.
                }
            }
        }

        for (Map.Entry<String, Map<Long, Long>> world : ownerByWorld.entrySet()) {
            final Map<Long, Long> owners = world.getValue();
            final Long2ObjectMap<ClaimEntry> claims = new Long2ObjectOpenHashMap<>(owners.size());
            for (Map.Entry<Long, Long> claim : owners.entrySet()) {
                final long chunkKey = claim.getKey();
                final long clanId = claim.getValue();
                final int chunkX = chunkX(chunkKey);
                final int chunkZ = chunkZ(chunkKey);
                claims.put(chunkKey, new ClaimEntry(clanId, ClaimEntry.sides(
                        sameOwner(owners, chunkX, chunkZ - 1, clanId),
                        sameOwner(owners, chunkX + 1, chunkZ, clanId),
                        sameOwner(owners, chunkX, chunkZ + 1, clanId),
                        sameOwner(owners, chunkX - 1, chunkZ, clanId))));
            }
            built.put(world.getKey(), claims);
        }

        byWorld = Collections.unmodifiableMap(built);
        generation++;
    }

    private boolean sameOwner(Map<Long, Long> owners, int chunkX, int chunkZ, long clanId) {
        final Long owner = owners.get(key(chunkX, chunkZ));
        return owner != null && owner == clanId;
    }
}
