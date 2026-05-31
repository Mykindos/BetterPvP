package me.mykindos.betterpvp.clans.world.veloran.gateway;

import dev.brauw.mapper.region.CuboidRegion;
import me.mykindos.betterpvp.core.scene.behavior.SceneBehavior;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Sundered Gate's entry trigger: each tick it detects players stepping <em>into</em> the gate cuboid and fires
 * {@link #onEnter(Player)} exactly once per entry.
 * <p>
 * Entry is edge-detected via {@link #inside}: a player already standing in the volume does not re-trigger every tick,
 * and only fires again after leaving and returning. The destination teleport itself is intentionally not wired yet —
 * {@link #onEnter(Player)} is the single seam where the Veloran teleport will live.
 */
public class GatewayTeleportBehavior implements SceneBehavior {

    private final CuboidRegion region;
    private final Set<UUID> inside = new HashSet<>();

    public GatewayTeleportBehavior(CuboidRegion region) {
        this.region = region;
    }

    @Override
    public void tick() {
        final World world = region.getWorld();
        if (world == null) {
            return;
        }

        for (Player player : world.getPlayers()) {
            final boolean nowInside = region.contains(player.getLocation());
            final boolean wasInside = inside.contains(player.getUniqueId());

            if (nowInside && !wasInside) {
                inside.add(player.getUniqueId());
                onEnter(player);
            } else if (!nowInside && wasInside) {
                inside.remove(player.getUniqueId());
            }
        }

        // Drop players who disconnected or changed worlds while inside, so the set can't leak.
        inside.removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    /**
     * Invoked once when {@code player} first steps into the gate volume.
     * <p>
     * Destination teleport wiring is deferred — this is the single seam where it will be added.
     *
     * @param player the player who just entered the gate
     */
    private void onEnter(Player player) {
        Bukkit.broadcastMessage("teleport");
    }

    @Override
    public void stop() {
        inside.clear();
    }
}
