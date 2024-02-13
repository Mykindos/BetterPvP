package me.mykindos.betterpvp.champions.weapons.impl.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.LegendaryWeapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class WingsOfZanzul extends Weapon implements LegendaryWeapon, Listener {
    @Inject
    public WingsOfZanzul(Champions champions) {
        super(champions, "wings_of_zanzul");
    }

    @EventHandler
    public void elytraDurability(PlayerItemDamageEvent event) {

        if(matches(event.getItem())) {
            event.setCancelled(true);
        }

    }

    @Override
    public List<Component> getLore(ItemStack item) {
        return new ArrayList<>();
    }
}
