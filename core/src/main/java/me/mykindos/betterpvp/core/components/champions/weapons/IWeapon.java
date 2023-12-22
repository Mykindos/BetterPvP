package me.mykindos.betterpvp.core.components.champions.weapons;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public interface IWeapon {

    Material getMaterial();
    Component getName();
    int getModel();
    List<Component> getLore();

    boolean isHoldingWeapon(Player player);

}
