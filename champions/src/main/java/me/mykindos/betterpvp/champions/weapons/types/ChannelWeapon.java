package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class ChannelWeapon extends Weapon implements IWeapon, Listener {

    protected final Set<UUID> active = new HashSet<>();

    public ChannelWeapon(String key) {
        super(key);
    }

    public ChannelWeapon(String key, List<Component> lore) {
        super(key, lore);
    }

    public abstract double getEnergy();

    public boolean useShield(Player player) {
        return false;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        active.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

}
