package me.mykindos.betterpvp.core.combat.hitbox;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;

@BPvPListener
public class HitboxHandler implements Listener {

    private static final double CREATIVE_ATTACK_REACH = 5.0D;
    private static final double SURVIVAL_ATTACK_REACH = 3.0D;

    @Inject
    private Core core;

    @Config(path = "pvp.hitboxExpansion", defaultValue = "0.0")
    @Inject
    private double hitboxExpansion = 0.0;

    @EventHandler
    public void onSwingBlock(PlayerInteractEvent event) {
        if (event.getAction().isLeftClick() && event.getHand() == EquipmentSlot.HAND) {
            hit(event.getPlayer());
        }
    }

    private void hit(Player player) {
        if (hitboxExpansion <= 0) {
            return;
        }

        final Location loc = player.getEyeLocation();
        double reach = player.getGameMode().equals(GameMode.CREATIVE) ? CREATIVE_ATTACK_REACH : SURVIVAL_ATTACK_REACH;
        final Vector attackStart = loc.toVector();
        final Vector direction = loc.getDirection();

        final Collection<Player> nearby = loc.getNearbyEntitiesByType(Player.class, reach + hitboxExpansion);
        nearby.remove(player);
        for (Player other : nearby) {
            final BoundingBox box = other.getBoundingBox().clone().expand(hitboxExpansion, 0, hitboxExpansion);

            // Get projection of the direction to the center of the hitbox
            // over the direction of the attack to shift the hitbox by that
            // vector and see if it overlaps with the start of the attack
            final Vector distance = box.getCenter().subtract(attackStart);
            final double projectionLength = direction.clone().dot(distance) / direction.length();
            final Vector projection = direction.clone().multiply(projectionLength);

            // Multiply the projection by -1 to get the vector from the center of the hitbox
            // to the start of the attack
            final BoundingBox shifted = box.clone().shift(projection.multiply(-1));
            if (shifted.contains(attackStart)) {
                player.attack(other);
                return;
            }
        }
    }

}

