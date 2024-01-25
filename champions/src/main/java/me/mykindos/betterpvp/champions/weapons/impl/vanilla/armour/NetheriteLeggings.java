package me.mykindos.betterpvp.champions.weapons.impl.vanilla.armour;

import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPVPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class NetheriteLeggings extends Weapon {
    public NetheriteLeggings() {
        super("warlock_leggings");
    }

    @Override
    public void loadWeapon(BPVPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"EEE", "E E", "E E"}, new Material[]{Material.NETHERITE_INGOT}, CraftingBookCategory.EQUIPMENT);
    }

}
