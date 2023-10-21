package me.mykindos.betterpvp.champions.listeners;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "true")
    private boolean critArrowsEnabled;

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "4.5")
    private double baseArrowDamage;

    private final HashMap<Arrow, Float> arrows = new HashMap<>();

    private final Champions champions;

    @Inject
    public ArrowListener(Champions champions) {
        this.champions = champions;
    }


    @UpdateEvent
    public void displayCritTrail() {
        Iterator<Map.Entry<Arrow, Float>> it = arrows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Arrow, Float> next = it.next();
            Arrow arrow = next.getKey();
            if (arrow == null) {
                it.remove();
            } else if (arrow.isDead()) {
                it.remove();
            } else {
                if (critArrowsEnabled && next.getValue() == 1.0) {
                    Location loc = arrow.getLocation().add(new Vector(0, 0.25, 0));
                    Particle.CRIT.builder().location(loc).count(3).extra(0).receivers(60, true).spawn();
                }
            }
        }
    }

    /**
     * Disable bow critical hits
     *
     * @param event The event
     */
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof Arrow arrow) {
            System.out.println(arrow.getVelocity().length() * 3);
            arrow.setCritical(false);
            if (event.getEntity() instanceof Player player) {
                arrow.setMetadata("ShotWith", new FixedMetadataValue(champions, player.getInventory().getItemInMainHand().getType().name()));
            }
            arrows.put(arrow, event.getForce());
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
