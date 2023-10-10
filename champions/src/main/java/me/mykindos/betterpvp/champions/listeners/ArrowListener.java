package me.mykindos.betterpvp.champions.listeners;

import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "true")
    private boolean critArrowsEnabled;

    private final Set<Arrow> arrows = new HashSet<>();


    @UpdateEvent
    public void displayCritTrail() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow next = it.next();
            if (next == null) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location loc = next.getLocation().add(new Vector(0, 0.25, 0));
                Particle.CRIT.builder().location(loc).count(3).extra(0).receivers(60, true).spawn();

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

            if (critArrowsEnabled) {
                // Only show particle for fully charged bows
                if (event.getForce() == 1.0) {
                    arrows.add(arrow);
                }
            }
        }
    }

}
