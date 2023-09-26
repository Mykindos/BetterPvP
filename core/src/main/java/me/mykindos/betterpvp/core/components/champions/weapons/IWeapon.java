package me.mykindos.betterpvp.core.components.champions.weapons;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public interface IWeapon {

    Material getMaterial();
    Component getName();
    List<Component> getLore();

    void activate(Player player);
    boolean canUse(Player player);

}
