package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

@Singleton
@BPvPListener
public class WingsOfZanzul extends Weapon implements LegendaryWeapon, Listener {
    public WingsOfZanzul() {
        super("wings_of_zanzul");
    }

    @EventHandler
    public void elytraDurability(PlayerItemDamageEvent event) {

        if(matches(event.getItem())) {
            event.setCancelled(true);
        }

    }

}
