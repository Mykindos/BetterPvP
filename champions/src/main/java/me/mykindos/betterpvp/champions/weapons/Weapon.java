package me.mykindos.betterpvp.champions.weapons;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.itemstack.BPvPCustomItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@Singleton
public abstract class Weapon extends BPvPCustomItem implements IWeapon {



    public Weapon(String key) {
        this(key, null);
    }

    public Weapon(String key, List<Component> lore) {
        super("champions", key, Material.DEBUG_STICK, Component.text("Unintialized Weapon"), lore, 2);
    }

    public void loadWeapon(BPVPItem item) {
        setMaterial(item.getMaterial());
        setName(item.getName());
        setLore(item.getLore());
        setCustomModelData(item.getCustomModelData());
    }

    @Override
    public boolean isHoldingWeapon(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return matches(itemStack);
    }

    public int getModel() {
        return getCustomModelData();
    }
}
