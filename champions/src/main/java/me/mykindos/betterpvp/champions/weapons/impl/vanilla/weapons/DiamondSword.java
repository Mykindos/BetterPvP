package me.mykindos.betterpvp.champions.weapons.impl.vanilla.weapons;

import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class DiamondSword extends Weapon {
    public DiamondSword() {
        super("power_sword");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"D", "D", "S"}, new Material[]{Material.DIAMOND_BLOCK, Material.STICK}, CraftingBookCategory.EQUIPMENT);

    }

}
