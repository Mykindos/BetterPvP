package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shows only the <em>nearest</em> label of a multi-label resource node to each player. A node may have several label
 * markers; the operator places them where they want, and each player sees just the closest within view range.
 * <p>
 * Performance: a single throttled pass (not per-node listeners), with three cost guards — (1) a coarse per-group
 * bounding-sphere reject so distant groups are skipped with one squared-distance check, (2) only players in the
 * group's world are considered, and (3) per-player visibility packets are sent <em>only on change</em> (tracked in
 * {@code shown}). Single-label nodes are never registered here and just render normally.
 */
@Singleton
@BPvPListener
public class ResourceNodeLabelService implements Listener {

    /** Max distance (blocks) at which a label is eligible to be the visible one. */
    private static final double VIEW = 48.0;
    private static final double VIEW_SQ = VIEW * VIEW;

    private final Core core;
    private final List<LabelGroup> groups = new CopyOnWriteArrayList<>();

    @Inject
    public ResourceNodeLabelService(@NotNull Core core) {
        this.core = core;
    }

    /**
     * Registers a node's labels for closest-only visibility. Call only when {@code prop.getLabels().size() > 1}.
     */
    public void register(@NotNull ResourceNodeProp prop) {
        groups.add(new LabelGroup(prop.getLabels()));
    }

    public void clear() {
        groups.clear();
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        for (LabelGroup group : groups) {
            updateGroup(group);
        }
    }

    private void updateGroup(@NotNull LabelGroup group) {
        if (group.displays.isEmpty()) {
            return;
        }
        final World world = group.displays.get(0).getWorld();
        if (world == null) {
            return;
        }

        final double cull = group.radius + VIEW;
        final double cullSq = cull * cull;

        for (Player player : world.getPlayers()) {
            final UUID id = player.getUniqueId();
            final double dx = player.getX() - group.centerX;
            final double dy = player.getY() - group.centerY;
            final double dz = player.getZ() - group.centerZ;
            if (dx * dx + dy * dy + dz * dz > cullSq) {
                group.shown.remove(id); // out of every label's view — client culls; re-resolve on return
                continue;
            }

            int closest = -1;
            double best = VIEW_SQ;
            for (int i = 0; i < group.displays.size(); i++) {
                final double lx = player.getX() - group.labelX[i];
                final double ly = player.getY() - group.labelY[i];
                final double lz = player.getZ() - group.labelZ[i];
                final double distance = lx * lx + ly * ly + lz * lz;
                if (distance < best) {
                    best = distance;
                    closest = i;
                }
            }

            final Integer current = group.shown.get(id);
            if (closest == -1) {
                if (current != null) {
                    player.hideEntity(core, group.displays.get(current));
                    group.shown.remove(id);
                }
                continue;
            }
            if (current == null) {
                // First time in range: hide all, reveal only the closest.
                for (TextDisplay display : group.displays) {
                    player.hideEntity(core, display);
                }
                player.showEntity(core, group.displays.get(closest));
                group.shown.put(id, closest);
            } else if (current != closest) {
                player.hideEntity(core, group.displays.get(current));
                player.showEntity(core, group.displays.get(closest));
                group.shown.put(id, closest);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID id = event.getPlayer().getUniqueId();
        for (LabelGroup group : groups) {
            group.shown.remove(id);
        }
    }

    /**
     * A node's labels plus a precomputed bounding sphere for coarse culling and per-player visibility state. Labels are
     * {@link TextDisplay}s that never move, so their coordinates are captured once here and reused every pass rather
     * than re-fetched (and re-allocated) via {@code getLocation()} on the hot path.
     */
    private static final class LabelGroup {
        private final List<TextDisplay> displays;
        private final double[] labelX;
        private final double[] labelY;
        private final double[] labelZ;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;
        private final ConcurrentHashMap<UUID, Integer> shown = new ConcurrentHashMap<>();

        private LabelGroup(List<TextDisplay> displays) {
            this.displays = List.copyOf(displays);
            final int count = this.displays.size();
            this.labelX = new double[count];
            this.labelY = new double[count];
            this.labelZ = new double[count];
            double sx = 0;
            double sy = 0;
            double sz = 0;
            for (int i = 0; i < count; i++) {
                final Location location = this.displays.get(i).getLocation();
                labelX[i] = location.getX();
                labelY[i] = location.getY();
                labelZ[i] = location.getZ();
                sx += labelX[i];
                sy += labelY[i];
                sz += labelZ[i];
            }
            this.centerX = sx / count;
            this.centerY = sy / count;
            this.centerZ = sz / count;
            double maxRadius = 0;
            for (int i = 0; i < count; i++) {
                final double dx = labelX[i] - centerX;
                final double dy = labelY[i] - centerY;
                final double dz = labelZ[i] - centerZ;
                maxRadius = Math.max(maxRadius, Math.sqrt(dx * dx + dy * dy + dz * dz));
            }
            this.radius = maxRadius;
        }
    }
}
