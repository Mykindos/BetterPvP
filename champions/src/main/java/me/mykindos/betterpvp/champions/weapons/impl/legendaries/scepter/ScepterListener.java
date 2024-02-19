package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@BPvPListener
@Singleton
public class ScepterListener implements Listener {

    @Inject
    private Scepter scepter;

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (scepter.isHoldingWeapon(damager)) {
            event.setDamage(scepter.getBaseDamage());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isLeftClick()) {
            return;
        }

        if (scepter.isHoldingWeapon(event.getPlayer())) {
            scepter.tryUseBeam(event.getPlayer());
        }
    }

    @UpdateEvent
    public void doBeam() {
        final Iterator<Map.Entry<Player, List<MeridianBeam>>> iterator = scepter.beams.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<MeridianBeam>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<MeridianBeam> beams = cur.getValue();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<MeridianBeam> beamIterator = beams.iterator();
            while (beamIterator.hasNext()) {
                final MeridianBeam beam = beamIterator.next();
                if (beam.isMarkForRemoval()) {
                    beamIterator.remove();
                    continue;
                }

                beam.tick();
            }

            if (beams.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @UpdateEvent
    public void doBlackHole() {
        final Iterator<Map.Entry<Player, List<BlackHole>>> iterator = scepter.blackHoles.entrySet().iterator();

        while (iterator.hasNext()) {
            final Map.Entry<Player, List<BlackHole>> cur = iterator.next();
            final Player player = cur.getKey();
            final List<BlackHole> blackHoles = cur.getValue();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            final Iterator<BlackHole> holes = blackHoles.iterator();
            while (holes.hasNext()) {
                final BlackHole hole = holes.next();
                if (hole.isMarkForRemoval()) {
                    holes.remove();
                    continue;
                }

                hole.tick();
            }

            if (blackHoles.isEmpty()) {
                iterator.remove();
            }
        }
    }

}
