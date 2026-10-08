package me.mykindos.betterpvp.core.scene.mob.ai.component;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import me.mykindos.betterpvp.core.scene.mob.ai.AIComponent;
import me.mykindos.betterpvp.core.scene.mob.ai.AIControl;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * Faces the nearest player within a radius while the mob isn't pathing, keeping to one player until they leave.
 */
public class LookAtNearbyPlayerComponent implements AIComponent {

    private static final int SEARCH_TICKS = 5;

    private final SceneMob mob;

    private double radius = 5.0;
    @Nullable private Player player;
    private int searchCounter;

    public LookAtNearbyPlayerComponent(SceneMob mob) {
        this.mob = mob;
    }

    /** Distance, in blocks, within which a player is looked at. */
    public LookAtNearbyPlayerComponent radius(double radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public EnumSet<AIControl> getControls() {
        return EnumSet.of(AIControl.LOOK);
    }

    @Override
    public boolean canStart() {
        return !mob.getNavigator().isNavigating();
    }

    @Override
    public void tick() {
        final Mob bukkitMob = mob.getBukkitMob();
        if (bukkitMob == null) {
            return;
        }
        if (searchCounter-- <= 0) {
            searchCounter = SEARCH_TICKS - 1;
            final Location at = bukkitMob.getLocation();
            final Collection<Player> nearby = bukkitMob.getWorld().getNearbyPlayers(at, radius);
            if (player == null || !player.isValid() || !nearby.contains(player)) {
                player = nearby.stream()
                        .min(Comparator.comparingDouble(candidate -> candidate.getLocation().distanceSquared(at)))
                        .orElse(null);
            }
        }
        if (player != null) {
            bukkitMob.lookAt(player.getEyeLocation());
        }
    }

    @Override
    public void stop() {
        player = null;
        searchCounter = 0;
    }

}
