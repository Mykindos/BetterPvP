package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class HyperAxe extends Weapon implements LegendaryWeapon, Listener {

    public HyperAxe() {
        super("hyper_axe");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(CustomDamageEvent event) {

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isHoldingWeapon(player)) return;

        event.setDamage(4);
        event.setKnockback(false);
        event.setDamageDelay(50);

    }

}
