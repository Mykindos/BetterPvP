package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Schedules and drives the {@link TreeRespawnManager}.
 *
 * <h3>Two-phase approach</h3>
 * <ol>
 *   <li><b>Scanner</b> (every 5 minutes) – iterates all currently loaded chunks in
 *       the main world and enqueues any unclaimed chunk that needs evaluation.</li>
 *   <li><b>Worker</b> (every 1 second) – dequeues and processes exactly one chunk,
 *       keeping the main-thread impact to an absolute minimum.</li>
 * </ol>
 *
 * <h3>Performance notes</h3>
 * <ul>
 *   <li>Chunks are <b>never force-loaded</b>; we only ever visit chunks that the
 *       server has already loaded for player activity.</li>
 *   <li>Processing is deliberately throttled to one chunk per second so that the
 *       cumulative cost of block reads and (occasional) tree generation is spread
 *       out and never spikes the server TPS.</li>
 *   <li>The queue caps at {@code trees.respawn.max-queue-size} entries to bound
 *       memory when many chunks are loaded simultaneously.</li>
 * </ul>
 */
@BPvPListener
@Singleton
public class TreeRespawnListener implements Listener {

    private final TreeRespawnManager treeRespawnManager;

    @Inject
    public TreeRespawnListener(TreeRespawnManager treeRespawnManager) {
        this.treeRespawnManager = treeRespawnManager;
    }

    /**
     * Every 5 minutes: scan all loaded chunks in the main world and add unclaimed,
     * potentially deforested chunks to the respawn queue.
     *
     * <p>The chunk array is <b>shuffled</b> before iteration so that when the queue
     * fills up ({@code trees.respawn.max-queue-size}), every chunk has an equal
     * chance of being selected — preventing the same set of "early-in-iteration-order"
     * chunks from always winning while later ones are starved indefinitely.
     */
    @UpdateEvent(delay = 300_000L) // 5 minutes
    public void scanLoadedChunks() {
        if (!treeRespawnManager.isEnabled()) return;

        World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
        if (world == null) return;

        // Shuffle so every chunk gets a fair shot when the queue cap is hit
        List<Chunk> chunks = Arrays.asList(world.getLoadedChunks());
        Collections.shuffle(chunks);

        for (Chunk chunk : chunks) {
            treeRespawnManager.queueChunk(chunk);
        }
    }

    /**
     * Every 1 second: process one chunk from the queue.
     *
     * <p>One chunk per second means that even a full queue of 200 chunks is
     * drained in ~3 minutes, well within the 5-minute scan cycle.
     */
    @UpdateEvent(delay = 1_000L) // 1 second
    public void processNextChunk() {
        treeRespawnManager.processNext();
    }
}



