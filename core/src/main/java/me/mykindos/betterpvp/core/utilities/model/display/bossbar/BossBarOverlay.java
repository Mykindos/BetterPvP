package me.mykindos.betterpvp.core.utilities.model.display.bossbar;

import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.TimedDisplayObject;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages HUD overlay graphics displayed to a player via the WHITE boss bar slot.
 *
 * <p>All active overlays are composited into a single boss bar name component each tick,
 * separated by {@code Component.translatable("newlayer").font(Resources.Font.SPACE)} to
 * allow the resource pack to layer them vertically on screen.
 *
 * <p>Unlike {@link BossBarQueue}, there is no priority ordering — overlays are rendered
 * in insertion order. Any overlay whose provider returns {@code null} this tick is silently
 * skipped (but remains in the list for future ticks). Overlays can be permanent
 * ({@link DisplayObject}) or time-limited ({@link TimedDisplayObject}).
 */
public class BossBarOverlay {

    private static final Component SEPARATOR =
            Component.translatable("newlayer").font(Resources.Font.SPACE);

    private final List<DisplayObject<Component>> overlays = new ArrayList<>();
    private final BossBar bar = BossBar.bossBar(
            Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

    private final Object lock = new Object();

    // -------------------------------------------------------------------------
    // Management
    // -------------------------------------------------------------------------

    /**
     * Adds an overlay to the list.
     *
     * <p>If the overlay is a {@link TimedDisplayObject} and {@code waitToExpire} is
     * {@code false}, its expiry timer is started immediately.
     */
    public void add(DisplayObject<Component> overlay) {
        synchronized (lock) {
            overlays.add(overlay);
            if (overlay instanceof TimedDisplayObject<?> timed && !timed.isWaitToExpire()) {
                timed.startTime();
            }
        }
    }

    /** Removes a specific overlay from the list. */
    public void remove(DisplayObject<Component> overlay) {
        synchronized (lock) {
            overlays.remove(overlay);
        }
    }

    /** Removes all overlays. */
    public void clear() {
        synchronized (lock) {
            overlays.clear();
        }
    }

    /** @return {@code true} if there is at least one overlay in the list. */
    public boolean hasOverlays() {
        synchronized (lock) {
            return !overlays.isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Composites all active overlays into the boss bar name and shows it to the player.
     *
     * <p>Steps each tick:
     * <ol>
     *   <li>Remove expired/invalid overlays.</li>
     *   <li>For each remaining overlay, call its provider. Skip {@code null} results.</li>
     *   <li>Start the timer on {@link TimedDisplayObject} entries on first display.</li>
     *   <li>Join all non-null components with {@link #SEPARATOR}.</li>
     *   <li>If the composite is non-empty, update the bar name and add the player as viewer;
     *       otherwise remove the player as viewer.</li>
     * </ol>
     */
    public void show(Gamer gamer) {
        cleanUp();
        final Player player = Bukkit.getPlayer(UUID.fromString(gamer.getUuid()));
        if (player == null) return;

        Component combined = null;
        synchronized (lock) {
            for (DisplayObject<Component> overlay : overlays) {
                Component component = overlay.getProvider().apply(gamer);
                if (component == null) continue;

                if (overlay instanceof TimedDisplayObject<?> timed) {
                    timed.startTime();
                }

                if (combined == null) {
                    combined = component;
                } else {
                    combined = combined.append(SEPARATOR).append(component);
                }
            }
        }

        if (combined == null) {
            combined = Component.empty();
        }

        bar.name(combined);
        bar.addViewer(player);
    }

    /** Removes expired/invalid overlays from the list. */
    public void cleanUp() {
        synchronized (lock) {
            overlays.removeIf(DisplayObject::isInvalid);
        }
    }

    /**
     * Removes the player as a viewer and clears all overlays.
     * Call this when the owning player disconnects or the display is no longer needed.
     */
    public void hide(Player player) {
        bar.removeViewer(player);
        clear();
    }
}
