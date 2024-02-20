package me.mykindos.betterpvp.champions.listeners;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import javax.inject.Inject;
import java.util.HashMap;

@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "true")
    private boolean critArrowsEnabled;

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "6.0")
    private double baseArrowDamage;

    private final HashMap<Arrow, Float> arrows = new HashMap<>();

    private final Champions champions;
    private final RoleManager roleManager;

    @Inject
    public ArrowListener(Champions champions, RoleManager roleManager) {
        this.champions = champions;
        this.roleManager = roleManager;
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof Arrow arrow) {
            if (event.getEntity() instanceof Player player) {
                arrow.setMetadata("ShotWith", new FixedMetadataValue(champions, player.getInventory().getItemInMainHand().getType().name()));
            }
            arrows.put(arrow, event.getForce());
        }
    }

    @EventHandler
    public void onRangerArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!(roleManager.hasRole(player, Role.RANGER))) return;
        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof LivingEntity damagee) {
            if(damagee instanceof Player && ((Player) damagee).getGameMode() == GameMode.CREATIVE) return;
            damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.5f);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBaseArrowDamage(CustomDamageEvent event) {
        if (event.getProjectile() instanceof Arrow) {
            event.setDamage(baseArrowDamage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArrowDamage(CustomDamageEvent event) {
        if (event.getProjectile() instanceof Arrow arrow) {
            if (arrows.containsKey(arrow)) {
                float dmgMultiplier = arrows.get(arrow);
                event.setDamage(event.getDamage() * dmgMultiplier);
                arrows.remove(arrow);
            }
        }

    }

    /*
     * Removes arrows when they hit the ground, or a player
     */
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            UtilServer.runTaskLater(champions, arrow::remove, 5L);
        }
    }

}
