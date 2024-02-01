package me.mykindos.betterpvp.core.combat.weapon;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import me.mykindos.betterpvp.core.items.BPVPItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@Singleton
public abstract class Weapon extends BPVPItem implements IWeapon {

    public Weapon(String key) {
        this(key, (List<Component>) null);
    }

    public Weapon(String key, List<Component> lore) {
        this("champions", key, lore);
    }

    public Weapon(String namespace, String key) {
        this(namespace, key, null);
    }

    public Weapon(String namespace, String key, List<Component> lore) {
        super(namespace, key, Material.DEBUG_STICK, Component.text("Unintialized Weapon"), lore, 0, 2, false, true);
    }

    public void loadWeapon(BPVPItem item) {
        setMaterial(item.getMaterial());
        setName(item.getName());
        setLore(item.getLore());
        setMaxDurability(item.getMaxDurability());
        setCustomModelData(item.getCustomModelData());
        setGlowing(item.isGlowing());
        setGiveUUID(item.isGiveUUID());
    }

    @Override
    public boolean isHoldingWeapon(Player player) {
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        return matches(itemStack);
    }

    @Override
    public List<Component> getLore(ItemStack itemStack) {
        return getLore();
    }

    public int getModel() {
        return getCustomModelData();
    }
}
