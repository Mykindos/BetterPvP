package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
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
