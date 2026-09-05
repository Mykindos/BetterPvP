package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Floats the remaining respawn on every depleted ore block, on the faces of it you can actually see.
 * <p>
 * Applies to every ore node, not just the starter mine: a field you have just stripped otherwise gives no clue whether
 * it is worth waiting at or worth leaving, and the answer to that is the whole of the decision the node is asking you
 * to make.
 *
 * <h3>Two kinds of node, one loop</h3>
 * A node with shared state gets <b>one display per face</b> that everybody reads, because everyone is waiting on the
 * same block. A per-player node gets <b>a display per face per viewer</b>, hidden from everyone else, because two
 * people standing together are waiting on different things. The service does not branch on which archetype it is
 * looking at - it asks {@link ResourceArchetype#respawningPoints} for a viewer and renders the answer - and only
 * {@link ResourceArchetype#timersArePerPlayer()} decides which of the two loops a node goes through.
 *
 * <h3>Keeping the entity count sane</h3>
 * Three things bound it. Only <b>uncovered</b> faces get a display, so a block buried in a wall shows one face rather
 * than six and a fully-enclosed one shows none. Displays carry a short {@link Display#setViewRange view range}, so the
 * client stops drawing them well before it would a nameplate - the same trick the node labels use. And a per-player
 * node additionally keeps only the {@value #MAX_POINTS_PER_VIEWER} nearest points within {@value #VIEW_DISTANCE}
 * blocks of that player, since their count would otherwise scale with the population rather than with the mine.
 * <p>
 * Text is only re-sent when the rendered string actually changes, which throttles a half-second sweep down to the once
 * per second the countdown can visibly tick.
 */
@Singleton
@BPvPListener
public class ResourceNodeTimerService implements Listener {

    /** How close a player must be for one of their own countdowns to be drawn at all. */
    private static final double VIEW_DISTANCE = 12.0;
    private static final double VIEW_DISTANCE_SQ = VIEW_DISTANCE * VIEW_DISTANCE;

    /** Per-player cap, so a crowded mine costs entities in proportion to the mine and not to the crowd. */
    private static final int MAX_POINTS_PER_VIEWER = 8;

    /** {@code 1.0} is the vanilla ~64-block draw distance, so this is the view distance expressed as the client sees it. */
    private static final float DISPLAY_VIEW_RANGE = (float) (VIEW_DISTANCE / 64.0);

    /** Off the face by half a block plus a hair, so the text sits on the surface without z-fighting it. */
    private static final double FACE_OFFSET = 0.51;

    private static final float TEXT_SCALE = 0.5f;

    private static final TextColor FRESHLY_MINED = TextColor.color(0xFF5555);
    private static final TextColor ALMOST_BACK = TextColor.color(0x55FF55);

    private static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN
    };

    private final Clans clans;
    private final ResourceNodeManager nodes;

    /** Countdowns everyone shares, per node. */
    private final Map<Integer, Map<Marker, TextDisplay>> shared = new HashMap<>();

    /** Countdowns belonging to one player, hidden from everyone else. */
    private final Map<UUID, Map<Marker, TextDisplay>> personal = new HashMap<>();

    @Inject
    public ResourceNodeTimerService(@NotNull Clans clans, @NotNull ResourceNodeManager nodes) {
        this.clans = clans;
        this.nodes = nodes;
    }

    @UpdateEvent(delay = 500)
    public void refresh() {
        final Collection<ResourceNodeProp> loaded = nodes.nodes();
        refreshShared(loaded);
        refreshPersonal(loaded);
    }

    /** A player's countdowns are theirs alone, so they leave with them. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final Map<Marker, TextDisplay> markers = personal.remove(event.getPlayer().getUniqueId());
        if (markers != null) {
            markers.values().forEach(TextDisplay::remove);
        }
    }

    private void refreshShared(@NotNull Collection<ResourceNodeProp> loaded) {
        final Set<Integer> live = new HashSet<>();
        for (ResourceNodeProp node : loaded) {
            if (node.getArchetype().timersArePerPlayer()) {
                continue;
            }
            live.add(node.getId());
            final World world = node.getZone().getWorld();
            if (world == null) {
                continue;
            }
            reconcile(shared.computeIfAbsent(node.getId(), id -> new HashMap<>()),
                    desired(world, node.getArchetype().respawningPoints(node, null)), world, null);
        }
        // A node that has been unloaded or re-authored away takes its countdowns with it.
        dropStale(shared, live::contains);
    }

    private void refreshPersonal(@NotNull Collection<ResourceNodeProp> loaded) {
        final List<ResourceNodeProp> perPlayer = new ArrayList<>();
        for (ResourceNodeProp node : loaded) {
            if (node.getArchetype().timersArePerPlayer()) {
                perPlayer.add(node);
            }
        }
        if (perPlayer.isEmpty()) {
            dropStale(personal, id -> false);
            return;
        }

        final Set<UUID> live = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            final World world = player.getWorld();
            final Map<Marker, Countdown> wanted = new HashMap<>();
            for (ResourceNodeProp node : perPlayer) {
                if (!world.equals(node.getZone().getWorld())) {
                    continue;
                }
                wanted.putAll(desired(world, nearest(player, node.getArchetype().respawningPoints(node, player))));
            }
            if (wanted.isEmpty() && !personal.containsKey(player.getUniqueId())) {
                continue; // the common case: someone who has never mined anywhere near here
            }
            live.add(player.getUniqueId());
            reconcile(personal.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>()), wanted, world, player);
        }
        dropStale(personal, live::contains);
    }

    /**
     * The nearest points a viewer is close enough to read. Sorting only ever runs over one player's own depleted
     * points, which is bounded by the size of the mine.
     */
    private static @NotNull Collection<RespawnPoint> nearest(@NotNull Player player,
                                                            @NotNull Collection<RespawnPoint> points) {
        final List<RespawnPoint> inRange = new ArrayList<>();
        for (RespawnPoint point : points) {
            if (distanceSq(player, point) <= VIEW_DISTANCE_SQ) {
                inRange.add(point);
            }
        }
        if (inRange.size() <= MAX_POINTS_PER_VIEWER) {
            return inRange;
        }
        inRange.sort((left, right) -> Double.compare(distanceSq(player, left), distanceSq(player, right)));
        return inRange.subList(0, MAX_POINTS_PER_VIEWER);
    }

    private static double distanceSq(@NotNull Player player, @NotNull RespawnPoint point) {
        final double dx = player.getX() - (point.getX() + 0.5);
        final double dy = player.getY() - (point.getY() + 0.5);
        final double dz = player.getZ() - (point.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }

    /** Expands each regrowing point into one marker per uncovered face. */
    private static @NotNull Map<Marker, Countdown> desired(@NotNull World world,
                                                           @NotNull Collection<RespawnPoint> points) {
        final Map<Marker, Countdown> markers = new HashMap<>();
        for (RespawnPoint point : points) {
            // A display cannot be spawned into a chunk that is not there, and nobody is looking at it if it is not.
            if (!world.isChunkLoaded(point.getX() >> 4, point.getZ() >> 4)) {
                continue;
            }
            final Block block = world.getBlockAt(point.getX(), point.getY(), point.getZ());
            final Countdown countdown = new Countdown(point.getRemainingMs(), point.getTotalMs());
            for (BlockFace face : FACES) {
                if (isUncovered(block, face)) {
                    markers.put(new Marker(point.getX(), point.getY(), point.getZ(), face), countdown);
                }
            }
        }
        return markers;
    }

    /**
     * A face is uncovered when nothing solid is pressed against it. Non-occluding neighbours (glass, slabs, fences,
     * water) still count as uncovered, since the text reads through them perfectly well.
     */
    private static boolean isUncovered(@NotNull Block block, @NotNull BlockFace face) {
        return !block.getRelative(face).getType().isOccluding();
    }

    /**
     * Brings one set of displays in line with what should be showing: spawn what is new, remove what is gone, and
     * re-text what stayed.
     *
     * @param owner the only player who may see these, or null for countdowns everybody shares
     */
    private void reconcile(@NotNull Map<Marker, TextDisplay> existing, @NotNull Map<Marker, Countdown> wanted,
                           @NotNull World world, @Nullable Player owner) {
        final Iterator<Map.Entry<Marker, TextDisplay>> current = existing.entrySet().iterator();
        while (current.hasNext()) {
            final Map.Entry<Marker, TextDisplay> entry = current.next();
            // A chunk unload takes non-persistent displays with it, so a dead entity is expected, not an error.
            if (!wanted.containsKey(entry.getKey()) || !entry.getValue().isValid()) {
                entry.getValue().remove();
                current.remove();
            }
        }
        for (Map.Entry<Marker, Countdown> entry : wanted.entrySet()) {
            final TextDisplay display = existing.computeIfAbsent(entry.getKey(),
                    marker -> spawn(world, marker, owner));
            apply(display, entry.getValue());
        }
    }

    private void dropStale(@NotNull Map<?, Map<Marker, TextDisplay>> registry,
                           @NotNull Predicate<Object> live) {
        final Iterator<? extends Map.Entry<?, Map<Marker, TextDisplay>>> entries = registry.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry<?, Map<Marker, TextDisplay>> entry = entries.next();
            if (live.test(entry.getKey())) {
                continue;
            }
            entry.getValue().values().forEach(TextDisplay::remove);
            entries.remove();
        }
    }

    private @NotNull TextDisplay spawn(@NotNull World world, @NotNull Marker marker, @Nullable Player owner) {
        final TextDisplay display = world.spawn(marker.location(world), TextDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setBillboard(Display.Billboard.FIXED);
            spawned.setViewRange(DISPLAY_VIEW_RANGE);
            spawned.setSeeThrough(false);
            spawned.setShadowed(false);
            spawned.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
            spawned.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f(),
                    new Vector3f(TEXT_SCALE),
                    new AxisAngle4f()));
            if (owner != null) {
                // Hidden from the world up front, rather than shown and then hidden from everybody else one at a
                // time — which would also flash it at the whole room for a tick.
                spawned.setVisibleByDefault(false);
            }
        });
        if (owner != null) {
            owner.showEntity(clans, display);
        }
        return display;
    }

    private void apply(@NotNull TextDisplay display, @NotNull Countdown countdown) {
        final Component text = countdown.render();
        if (!text.equals(display.text())) {
            display.text(text);
        }
    }

    /** One countdown's position: a block, and which of its faces this display is pinned to. */
    @Value
    private static class Marker {

        int x;
        int y;
        int z;
        BlockFace face;

        /** Centred on the face, pushed just clear of it, turned to face whoever is standing on that side. */
        private @NotNull Location location(@NotNull World world) {
            final Location location = new Location(world,
                    x + 0.5 + face.getModX() * FACE_OFFSET,
                    y + 0.5 + face.getModY() * FACE_OFFSET,
                    z + 0.5 + face.getModZ() * FACE_OFFSET);
            location.setYaw(yaw());
            location.setPitch(pitch());
            return location;
        }

        private float yaw() {
            return switch (face) {
                case NORTH -> 180f;
                case EAST -> 270f;
                case WEST -> 90f;
                default -> 0f; // SOUTH, and the vertical faces, which are turned by pitch instead
            };
        }

        private float pitch() {
            return switch (face) {
                case UP -> -90f;
                case DOWN -> 90f;
                default -> 0f;
            };
        }
    }

    /** How long is left, and how long there was, which together give both the text and its colour. */
    @Value
    private static class Countdown {

        long remainingMs;
        long totalMs;

        /**
         * {@code MMm SSs}, running red at the moment of mining through to green as it comes back, so a glance across a
         * worked-out wall reads as "nearly ready" or "just started" without anyone reading the numbers.
         */
        private @NotNull Component render() {
            final long totalSeconds = (remainingMs + 999) / 1000; // round up, so it never shows 00m 00s while waiting
            final String text = String.format("%02dm %02ds", totalSeconds / 60, totalSeconds % 60);
            final float progress = totalMs <= 0 ? 1f
                    : Math.clamp(1f - ((float) remainingMs / (float) totalMs), 0f, 1f);
            return Component.text(text, TextColor.lerp(progress, FRESHLY_MINED, ALMOST_BACK))
                    .shadowColor(ShadowColor.shadowColor(0, 0, 0, 130));
        }
    }
}
