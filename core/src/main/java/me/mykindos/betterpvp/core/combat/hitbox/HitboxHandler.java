package me.mykindos.betterpvp.core.combat.hitbox;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Objects;

@BPvPListener
@Singleton
public class HitboxHandler implements Listener {

    private static final double CREATIVE_ATTACK_REACH = 5.0D;
    private static final double SURVIVAL_ATTACK_REACH = 3.0D;

    @Inject
    private Core core;

    @Config(path = "pvp.hitboxExpansion", defaultValue = "0.0")
    @Inject
    private double hitboxExpansion = 0.0;

    @Inject
    private CooldownManager cooldownManager;

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

        if (!cooldownManager.use(player, "Hitbox", 0.4, false)) {
            return;
        }

        double reach = player.getGameMode().equals(GameMode.CREATIVE) ? CREATIVE_ATTACK_REACH : SURVIVAL_ATTACK_REACH;

        final Location attackStart = player.getEyeLocation();
        final Vector direction = attackStart.getDirection();

        final RayTraceResult entityHit = attackStart.getWorld().rayTraceEntities(
                attackStart,
                direction,
                reach,
                hitboxExpansion,
                entity -> entity instanceof LivingEntity && entity != player && player.canSee(entity) && (player.hasLineOfSight(entity) || player.hasLineOfSight(entity.getLocation()))
        );

        if (entityHit == null) {
            return; // No entity hit
        }

        final LivingEntity entity = (LivingEntity) Objects.requireNonNull(entityHit.getHitEntity());
        final RayTraceResult blockHit = attackStart.getWorld().rayTraceBlocks(attackStart,
                direction,
                reach,
                FluidCollisionMode.NEVER,
                true);

        final Vector startVector = attackStart.toVector();
        if (blockHit != null) {
            final Vector blockPosition = blockHit.getHitPosition();
            final Vector hitPosition = entityHit.getHitPosition().setY(blockPosition.getY());
            hitPosition.add(direction.multiply(hitboxExpansion));
            if (hitPosition.distanceSquared(startVector) > blockPosition.distanceSquared(startVector)) {
                return; // Return if the block is hit before the player
            }
        }

        if (!UtilLocation.isInFront(player, entity.getLocation()) && !UtilLocation.isInFront(player, entity.getEyeLocation())) {
            return; // Return if the entity is behind the player
        }

        player.attack(entity);
    }

}