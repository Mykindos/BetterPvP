package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

@Singleton
@BPvPListener
public class WingsOfZanzul extends Weapon implements LegendaryWeapon, Listener {
    public WingsOfZanzul() {
        super(Material.ELYTRA, 1, UtilMessage.deserialize("<orange>Wings of Zanzul"), "wings_of_zanzul");
    }

    @EventHandler
    public void elytraDurability(PlayerItemDamageEvent event) {

        if(event.getItem().getType() == Material.ELYTRA) {
            event.setCancelled(true);
        }

    }

}
