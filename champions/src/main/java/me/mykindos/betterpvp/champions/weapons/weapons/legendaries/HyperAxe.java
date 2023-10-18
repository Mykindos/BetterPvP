package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@Singleton
@BPvPListener
public class HyperAxe extends Weapon implements LegendaryWeapon, Listener {

    public HyperAxe() {
        super(Material.MUSIC_DISC_BLOCKS, 1, UtilMessage.deserialize("<orange>Hyper Axe"));
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
