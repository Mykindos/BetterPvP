package me.mykindos.betterpvp.progression.profession.mining.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.mykindos.betterpvp.core.framework.blockbreak.event.ScriptedBlockPlaceEvent;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared sphere-detonation primitive for mining interactions (Explosive Excavation,
 * Oreburst Charge). Carves an imperfect sphere via {@link UtilBlock#breakBlock} and
 * offers each candidate block to a caller-supplied predicate for ore placement, so
 * each interaction keeps independent gameplay shape (shell-only, shell+surface, etc.)
 * while sharing the iteration, recursion guard, silent-break marking, and event firing.
 *
 * <p>FX (particles, sounds) are intentionally <i>not</i> handled here — callers run
 * their own after the call.
 */
public final class MiningDetonation {

    private MiningDetonation() {}

    private static final Random RANDOM = new Random();

    /** Recursion guard: {@code Player#breakBlock} can fire BlockBreakEvent → InteractionListener
     *  → re-route to the caller. Without the guard one mine could chain indefinitely. */
    private static final Set<UUID> DETONATING = ConcurrentHashMap.newKeySet();

    /**
     * Locations of blocks currently being broken inside {@link #detonate}. Read by
     * {@code ExplosiveExcavationSilencer} (a {@code @PluginAdapter} listener in clans) which
     * flips {@code TerritoryInteractEvent#setInform(false)} for any matching block, suppressing
     * the "You cannot break X in Clan Y" spam that would otherwise fire once per skipped block.
     * The TTL is a safety net — entries are normally invalidated synchronously after each break.
     */
    private static final Cache<Location, Boolean> SILENT_BREAKS = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public static boolean isSilentBreak(Location location) {
        return SILENT_BREAKS.getIfPresent(location) != null;
    }

    public static void markSilent(Location location) {
        SILENT_BREAKS.put(location, Boolean.TRUE);
    }

    public static void unmarkSilent(Location location) {
        SILENT_BREAKS.invalidate(location);
    }

    /**
     * Per-block context handed to the placement predicate. {@code distSq} is the squared
     * offset from {@code center}; {@code shellThresholdSq} is the squared distance at which
     * a block crosses from the inner sphere into the outer ~30% shell.
     */
    public record Context(Player player,
                          Block block,
                          Location center,
                          double distSq,
                          double rSq,
                          double shellThresholdSq,
                          boolean broken) {}

    /**
     * Carves the sphere and offers each surviving block to {@code placementPredicate}; if
     * the predicate returns true and the per-block roll passes, fires a
     * {@link ScriptedBlockPlaceEvent} and (if not cancelled) replaces the block with an ore
     * from {@code oreSupplier}.
     *
     * @param player              the player responsible for the detonation
     * @param center              sphere center
     * @param radius              integer radius; jittered ±0.8 per block for an organic shape
     * @param oreChance           per-eligible-block probability (0.0–1.0) of ore replacement
     * @param oreSupplier         supplies the ore Material for a single replacement;
     *                            return {@code null} to skip just that placement
     * @param sourceTag           identifier passed to {@link ScriptedBlockPlaceEvent}
     * @param blockTagManager     used to skip player-placed blocks
     * @param skipCenter          true when the center block should be left alone
     *                            (e.g. Excavation's trigger block has already been broken)
     * @param placementPredicate  decides per-block whether to attempt ore placement
     */
    public static void detonate(Player player,
                                Location center,
                                int radius,
                                double oreChance,
                                Supplier<Material> oreSupplier,
                                String sourceTag,
                                BlockTagManager blockTagManager,
                                boolean skipCenter,
                                Predicate<Context> placementPredicate) {
        final UUID id = player.getUniqueId();
        if (!DETONATING.add(id)) return;
        try {
            final World world = center.getWorld();
            final double rSq = radius * radius;
            // Anything in the outer ~30% by squared distance is treated as crater shell.
            final double shellThresholdSq = rSq * 0.7;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        final double distSq = dx * dx + dy * dy + dz * dz;

                        // Per-block jitter on the cutoff produces an imperfect sphere.
                        final double jitter = (RANDOM.nextDouble() - 0.5) * 1.6;
                        if (distSq > rSq + jitter) continue;

                        final Block block = world.getBlockAt(
                                center.getBlockX() + dx,
                                center.getBlockY() + dy,
                                center.getBlockZ() + dz);

                        if (skipCenter && block.equals(center.getBlock())) continue;
                        if (!UtilBlock.isStoneBased(block)) continue;
                        // Never reprocess existing ore — protects previousData from leaking
                        // a stale ore into the next Fields temp registration.
                        if (UtilBlock.isOre(block.getType())) continue;
                        if (blockTagManager.isPlayerPlaced(block)) continue;

                        final Location key = block.getLocation();
                        SILENT_BREAKS.put(key, Boolean.TRUE);
                        boolean broken;
                        try {
                            broken = UtilBlock.breakBlock(player, block);
                        } finally {
                            SILENT_BREAKS.invalidate(key);
                        }
                        // Capture AFTER the break: in Fields, breakBlock can succeed via
                        // FieldsListener#onOreMine which reverts an active temp ore to stone.
                        // Reading before would freeze the stale ore data and leak it into
                        // the next Fields temp registration as previousData.
                        final BlockData previousData = block.getBlockData();

                        final Context ctx = new Context(player, block, center,
                                distSq, rSq, shellThresholdSq, broken);
                        if (!placementPredicate.test(ctx)) continue;
                        if (RANDOM.nextDouble() >= oreChance) continue;

                        final Material ore = oreSupplier.get();
                        if (ore == null) continue;
                        final BlockData oreData = ore.createBlockData();
                        final ScriptedBlockPlaceEvent event = new ScriptedBlockPlaceEvent(
                                player, block, previousData, oreData, sourceTag);
                        event.callEvent();
                        if (!event.isCancelled()) {
                            block.setBlockData(oreData);
                            UtilBlock.playBlockEffect(block, oreData);
                        }
                    }
                }
            }
        } finally {
            DETONATING.remove(id);
        }
    }
}
