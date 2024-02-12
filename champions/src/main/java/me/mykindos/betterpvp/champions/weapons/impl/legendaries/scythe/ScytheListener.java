package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
@BPvPListener
@Singleton
@Slf4j
public class ScytheListener implements Listener {

    @Inject
    private Scythe scythe;

    @Inject
    private ClientManager clientManager;

    @Inject
    private DamageLogManager damageLogManager;

    @Inject
    private CooldownManager cooldownManager;

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (scythe.isHoldingWeapon(damager)) {
            event.setDamage(scythe.baseDamage);
            UtilPlayer.health(damager, scythe.healPerHit);
        }
    }

    @UpdateEvent
    public void doBlackHole() {
        final Iterator<Map.Entry<Player, List<BlackHole>>> iterator = scythe.blackHoles.entrySet().iterator();

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
        }
    }

}
