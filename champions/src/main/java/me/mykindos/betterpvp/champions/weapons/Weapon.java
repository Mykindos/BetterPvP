package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Singleton
public abstract class Weapon implements IWeapon {

    private final Material material;
    private final Component name;
    private final int model;
    private final List<Component> lore;

    public Weapon(Material material, Component name) {
        this(material, name, 0, new ArrayList<>());
    }

    public Weapon(Material material, int model, Component name) {
        this(material, name, model, new ArrayList<>());
    }

    @Override
    public boolean isHoldingWeapon(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return matches(itemStack);
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) return false;
        if (model != 0) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                if (itemMeta.hasCustomModelData()) {
                    return itemMeta.getCustomModelData() == model;
                }
            }
        }
        return true;
    }
}
