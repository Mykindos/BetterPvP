package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.items.itemstack.BPvPCustomItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Getter
@Singleton
public abstract class Weapon extends BPvPCustomItem implements IWeapon {
    private final List<Component> lore;

    public Weapon(Material material, Component name, String key, int model, List<Component> lore) {
        super(new NamespacedKey("champions", key), name, material, model);
        this.lore = lore;
    }

    public Weapon(Material material, Component name, String key) {
        this(material, name, key, 0, new ArrayList<>());
    }

    public Weapon(Material material, int model, Component name, String key) {
        this(material, name, key, model, new ArrayList<>());
    }

    @Override
    public boolean isHoldingWeapon(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return matches(itemStack);
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) return false;
        if (getModel() != 0) {
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
