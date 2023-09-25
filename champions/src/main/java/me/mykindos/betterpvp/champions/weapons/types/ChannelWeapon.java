package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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

    public ChannelWeapon(Material material, Component name, List<Component> lore) {
        super(material, name, lore);
    }

    public ChannelWeapon(Material material, Component name) {
        super(material, name);
    }

    public abstract float getEnergy();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        active.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

}
