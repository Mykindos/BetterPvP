package me.mykindos.betterpvp.champions.weapons.weapons.legendaries;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.champions.weapons.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.List;

@Singleton
@BPvPListener
public class WingsOfZanzul extends Weapon implements LegendaryWeapon, Listener {
    public WingsOfZanzul() {
        super(Material.ELYTRA, 0, Component.text("Wings of Zanzul", NamedTextColor.RED));
    }

    @EventHandler
    public void elytraDurability(PlayerItemDamageEvent event) {

        if(event.getItem().getType() == Material.ELYTRA) {
            event.setCancelled(true);
        }

    }

}
