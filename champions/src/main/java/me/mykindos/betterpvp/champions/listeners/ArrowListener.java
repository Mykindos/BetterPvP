package me.mykindos.betterpvp.champions.listeners;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "true")
    private boolean critArrowsEnabled;

    private final HashMap<Arrow, Float> arrows = new HashMap<>();


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
            arrow.setCritical(false);

            arrows.put(arrow, event.getForce());


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

}
