package me.mykindos.betterpvp.core.utilities.model.display.bossbar;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityData;
import me.mykindos.betterpvp.core.utilities.model.data.PriorityDataBlockingQueue;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.TimedDisplayObject;
import net.kyori.adventure.bossbar.BossBar;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * A display queue for boss bars that processes all {@link BossBarColor} slots simultaneously.
 *
 * <p>Unlike single-channel queues (e.g. {@link me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar})
 * which show only the highest-priority element, {@code BossBarQueue} maintains one priority sub-queue
 * per {@link BossBarColor}. When {@link #show(Gamer)} is called, the highest-priority element from
 * <em>every</em> non-empty color slot is shown simultaneously, allowing multiple boss bars on screen
 * at once.
 *
 * <p>Typical usage:
 * <pre>{@code
 * bossBarQueue.add(5, BossBarColor.GREEN, new DisplayObject<>(gamer ->
 *     new BossBarData(Component.text("Health"), player.getHealth() / 20f)));
 * }</pre>
 *
 * <p>Note: this class intentionally does <em>not</em> implement {@link me.mykindos.betterpvp.core.utilities.model.display.IDisplayQueue}
 * because {@link #add} requires a {@link BossBarColor} parameter in addition to priority.
 */
public class BossBarQueue {

    /** One priority sub-queue per color slot. Created lazily on first use of that slot. */
    private final Map<BossBarColor, PriorityDataBlockingQueue<DisplayObject<BossBarData>>> queues =
            new EnumMap<>(BossBarColor.class);

    /** One Adventure BossBar instance per color slot. Created lazily and reused across ticks. */
    private final Map<BossBarColor, BossBar> bars = new EnumMap<>(BossBarColor.class);

    private final Object lock = new Object();

    // -------------------------------------------------------------------------
    // Queue management
    // -------------------------------------------------------------------------

    /**
     * Adds an element to the specified color slot's priority sub-queue.
     *
     * <p>If the element is a {@link TimedDisplayObject} and {@code waitToExpire} is {@code false},
     * its expiry timer is started immediately (matching the behaviour of
     * {@link me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar}).
     *
     * @param priority higher values are shown first within the same color slot
     * @param color    the boss bar color slot to target
     * @param element  the display object; its provider may return {@code null} to be skipped
     */
    public void add(int priority, BossBarColor color, DisplayObject<BossBarData> element) {
        synchronized (lock) {
            queues.computeIfAbsent(color, c -> new PriorityDataBlockingQueue<>(5)).put(priority, element);
            if (element instanceof TimedDisplayObject<?> timed && !timed.isWaitToExpire()) {
                timed.startTime();
            }
        }
    }

    /** Removes a specific element from the given color slot's sub-queue. */
    public void remove(BossBarColor color, DisplayObject<BossBarData> element) {
        synchronized (lock) {
            PriorityDataBlockingQueue<DisplayObject<BossBarData>> queue = queues.get(color);
            if (queue != null) {
                queue.removeIf(pair -> pair.getRight().equals(element));
            }
        }
    }

    /** Clears all elements from every color slot. */
    public void clear() {
        synchronized (lock) {
            queues.values().forEach(PriorityDataBlockingQueue::clear);
        }
    }

    /** Clears all elements from the specified color slot only. */
    public void clear(BossBarColor color) {
        synchronized (lock) {
            PriorityDataBlockingQueue<DisplayObject<BossBarData>> queue = queues.get(color);
            if (queue != null) {
                queue.clear();
            }
        }
    }

    /** @return {@code true} if any color slot has at least one queued element. */
    public boolean hasElementsQueued() {
        synchronized (lock) {
            return queues.values().stream().anyMatch(q -> !q.isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Processes all color slots simultaneously:
     * <ul>
     *   <li>Cleans up expired/invalid elements from every slot.</li>
     *   <li>For each slot that has a valid element, the highest-priority item is displayed
     *       in that slot's boss bar (adding the player as a viewer if needed).</li>
     *   <li>For each slot with no valid elements, the boss bar is hidden by removing the viewer.</li>
     * </ul>
     */
    public void show(Gamer gamer) {
        cleanUp();
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player == null) return;

        for (BossBarColor color : BossBarColor.values()) {
            final BossBarData data;
            synchronized (lock) {
                data = nextElement(color, gamer);
            }

            if (data == null) {
                // No valid element for this color — remove the player from the bar if it exists.
                BossBar bar = bars.get(color);
                if (bar != null) {
                    bar.removeViewer(player);
                }
            } else {
                // Create or update the bar and ensure the player is viewing it.
                BossBar bar = bars.computeIfAbsent(color, c ->
                        BossBar.bossBar(data.getName(), data.getProgress(), c.getAdventureColor(), data.getOverlay()));
                bar.name(data.getName());
                bar.progress(data.getProgress());
                bar.overlay(data.getOverlay());
                bar.addViewer(player);
            }
        }
    }

    /** Removes expired/invalid elements from every color slot's sub-queue. */
    public void cleanUp() {
        synchronized (lock) {
            queues.values().forEach(queue ->
                    queue.removeIf(pair -> pair.getRight().isInvalid()));
        }
    }

    /**
     * Removes the player as a viewer from all active boss bars and clears all queues.
     * Call this when the owning player disconnects or the queue is no longer needed.
     */
    public void hide(Player player) {
        bars.values().forEach(bar -> bar.removeViewer(player));
        clear();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the highest-priority valid {@link BossBarData} for the given color slot,
     * or {@code null} if the slot is empty or all elements' providers return {@code null}.
     */
    private BossBarData nextElement(BossBarColor color, Gamer gamer) {
        PriorityDataBlockingQueue<DisplayObject<BossBarData>> queue = queues.get(color);
        if (queue == null || queue.isEmpty()) {
            return null;
        }

        Pair<PriorityData, DisplayObject<BossBarData>> peekPair = queue.peek();
        DisplayObject<BossBarData> display = peekPair.getRight();
        BossBarData data = display.getProvider().apply(gamer);

        if (data == null) {
            // Peek element's provider returned null — search for the first valid one.
            Pair<PriorityData, DisplayObject<BossBarData>> pair = queue.stream()
                    .filter(p -> p.getRight().getProvider().apply(gamer) != null)
                    .findFirst()
                    .orElse(null);
            if (pair == null) {
                return null;
            }
            display = pair.getRight();
            data = display.getProvider().apply(gamer);
        }

        if (display instanceof TimedDisplayObject<?> timed && !timed.hasStarted()) {
            timed.startTime();
        }

        return data;
    }
}
