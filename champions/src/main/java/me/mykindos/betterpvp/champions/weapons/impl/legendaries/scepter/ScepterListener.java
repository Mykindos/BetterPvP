package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
    public void onDamage(PreDamageEvent event) {
        if (!scepter.isEnabled()) {
            return;
        }

        DamageEvent cde = event.getDamageEvent();

        if (cde.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(cde.getDamager() instanceof Player damager)) return;
        if (scepter.isHoldingWeapon(damager)) {
            cde.setDamage(scepter.getBaseDamage());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!scepter.isEnabled()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isLeftClick() || event.useItemInHand() == Event.Result.DENY) {
            return;
        }


        if (scepter.isHoldingWeapon(event.getPlayer())) {
            var checkUsageEvent = UtilServer.callEvent(new PlayerUseItemEvent(event.getPlayer(), scepter, true));
            if (checkUsageEvent.isCancelled()) {
                UtilMessage.simpleMessage(event.getPlayer(), "Restriction", "You cannot use this weapon here.");
                return;
            }

            scepter.tryUseBeam(event.getPlayer());
        }
    }

    @UpdateEvent
    public void doBeam() {
        if (!scepter.isEnabled()) {
            return;
        }

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

    @UpdateEvent(priority = 100)
    public void doBlackHole() {
        if (!scepter.isEnabled()) {
            return;
        }

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
