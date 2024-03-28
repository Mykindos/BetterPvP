package me.mykindos.betterpvp.core.components.champions.weapons;

import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public interface IWeapon {

    Material getMaterial();
    Component getName();
    int getModel();
    default List<Component> getLore(ItemMeta meta) {
        return new ArrayList<>();
    }
    boolean isHoldingWeapon(Player player);

    boolean isEnabled();

    String getSimpleName();

    void loadConfig(BPvPPlugin plugin);

    boolean matches(ItemStack itemStack);

    default String getConfigName() {
        return "weapons/standard";
    }

    default void onInitialize(ItemMeta meta) {};

}
