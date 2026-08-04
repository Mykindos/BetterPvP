package me.mykindos.betterpvp.core.scene.indicator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scene.SceneObject;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Keeps floating markers over the things they belong to.
 * <p>
 * Indicators follow moving targets, so they are eagerly bound and driven from here rather than made chunk-managed —
 * materialization keys off a frozen anchor, which is only right for something that stays put.
 * <p>
 * Position is updated every tick so a marker does not lag behind a walking player; visibility is re-evaluated on a
 * slower beat because it is the expensive half and nothing about it needs to be tick-accurate.
 */
@BPvPListener
@Singleton
public class IndicatorService implements Listener {

    private final Core core;
    private final List<TrackedIndicator> indicators = new CopyOnWriteArrayList<>();

    @Inject
    public IndicatorService(@NotNull Core core) {
        this.core = core;
    }

    /**
     * Puts a marker over {@code target} until it is removed or the target goes away.
     *
     * @param heightOffset how far above the target's feet it floats, in blocks
     */
    public @NotNull Indicator attach(@NotNull Entity target, @NotNull IndicatorStyle style, double heightOffset) {
        final TrackedIndicator indicator = new TrackedIndicator(
                () -> anchor(target, heightOffset),
                () -> !UtilEntity.isRemoved(target),
                target.getUniqueId(),
                style.create(anchor(target, heightOffset)));

        // Ridden rather than chased: a passenger is moved by the server with its vehicle, so the icon never lags a
        // step behind a sprinting player the way a once-per-tick teleport does.
        indicator.mounted = target.addPassenger(indicator.body.getEntity());
        indicators.add(indicator);
        return indicator;
    }

    /**
     * Puts a marker at a fixed spot — over a shop sign, a lectern, a quartermaster who never moves.
     * <p>
     * It outlives nothing in particular, so it stays until removed.
     */
    public @NotNull Indicator attachAt(@NotNull Location location, @NotNull IndicatorStyle style) {
        final Location fixed = location.clone();
        return track(() -> fixed, () -> true, null, style);
    }

    /**
     * As {@link #attachAt(Location, IndicatorStyle)}, but the spot is recomputed every tick — which is how a marker
     * bobs, circles or drifts without needing a behaviour attached to a body that has nowhere to put one.
     */
    public @NotNull Indicator attachAt(@NotNull Supplier<Location> location, @NotNull IndicatorStyle style) {
        return track(location, () -> true, null, style);
    }

    /** Takes down every indicator following {@code target}. */
    public void detach(@NotNull Entity target) {
        for (TrackedIndicator indicator : indicators) {
            if (target.getUniqueId().equals(indicator.targetId)) {
                indicator.remove();
            }
        }
    }

    private @NotNull Indicator track(@NotNull Supplier<Location> anchor, @NotNull BooleanSupplier alive,
                                     UUID targetId, @NotNull IndicatorStyle style) {
        final TrackedIndicator indicator = new TrackedIndicator(anchor, alive, targetId, style.create(anchor.get()));
        indicators.add(indicator);
        return indicator;
    }

    @UpdateEvent
    public void followTargets() {
        for (TrackedIndicator indicator : indicators) {
            // A target that died or logged out takes its marker with it, so nothing is left hanging in the air.
            if (!indicator.active || !indicator.alive.getAsBoolean()) {
                indicator.remove();
                continue;
            }
            // A mounted indicator is carried by its vehicle; teleporting it would fight the mount and dismount it.
            if (!indicator.mounted && indicator.body.isMaterialized()) {
                indicator.body.getEntity().teleport(indicator.anchor.get());
            }
        }
    }

    @UpdateEvent(delay = 1000)
    public void refreshVisibility() {
        for (TrackedIndicator indicator : indicators) {
            if (indicator.active) {
                indicator.applyVisibility();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        detach(event.getPlayer());
        // Whoever left is no longer a viewer of anything; drop them so a rejoin starts from a clean visible state.
        for (TrackedIndicator indicator : indicators) {
            indicator.hiddenFrom.remove(event.getPlayer().getUniqueId());
        }
    }

    private static @NotNull Location anchor(@NotNull Entity target, double heightOffset) {
        return target.getLocation().add(0, target.getHeight() + heightOffset, 0);
    }

    /**
     * One live marker. Visibility is tracked as who it is <em>hidden</em> from, because an entity is visible by default
     * — so an indicator nobody has filtered costs nothing and needs no first-time setup pass.
     */
    private final class TrackedIndicator implements Indicator {

        private final Supplier<Location> anchor;
        private final BooleanSupplier alive;
        private final UUID targetId;
        private final SceneObject body;
        private final Set<UUID> hiddenFrom = new HashSet<>();

        private Predicate<Player> filter = viewer -> true;
        private boolean active = true;
        private boolean mounted;

        private TrackedIndicator(@NotNull Supplier<Location> anchor, @NotNull BooleanSupplier alive,
                                 UUID targetId, @NotNull SceneObject body) {
            this.anchor = anchor;
            this.alive = alive;
            this.targetId = targetId;
            this.body = body;
        }

        @Override
        public void visibleTo(@NotNull Predicate<Player> filter) {
            this.filter = filter;
            applyVisibility();
        }

        @Override
        public void remove() {
            if (!active) {
                return;
            }
            active = false;
            body.remove();
            indicators.remove(this);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        private void applyVisibility() {
            if (!body.isMaterialized()) {
                return;
            }

            final Entity entity = body.getEntity();
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                final boolean shouldSee = filter.test(viewer);
                final boolean hidden = hiddenFrom.contains(viewer.getUniqueId());

                if (shouldSee && hidden) {
                    viewer.showEntity(core, entity);
                    hiddenFrom.remove(viewer.getUniqueId());
                } else if (!shouldSee && !hidden) {
                    viewer.hideEntity(core, entity);
                    hiddenFrom.add(viewer.getUniqueId());
                }
            }
        }
    }
}
