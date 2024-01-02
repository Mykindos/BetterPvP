package me.mykindos.betterpvp.champions.weapons.weapons.vanilla.armour;

import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.core.items.BPVPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class NetheriteBoots extends Weapon {
    public NetheriteBoots() {
        super("warlock_boots");
    }

    @Override
    public void loadWeapon(BPVPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"E E", "E E"}, new Material[]{Material.NETHERITE_INGOT}, CraftingBookCategory.EQUIPMENT);

    }

}
