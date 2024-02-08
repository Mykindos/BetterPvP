package me.mykindos.betterpvp.core.components.champions.weapons;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface IWeapon {

    Material getMaterial();
    Component getName();
    int getModel();
    List<Component> getLore(ItemStack item);
    boolean isHoldingWeapon(Player player);

    boolean isEnabled();

    String getSimpleName();

    void loadConfig(BPvPPlugin plugin);

    boolean matches(ItemStack itemStack);

}
