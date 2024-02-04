package me.mykindos.betterpvp.champions.weapons.impl.vanilla.weapons;

import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class GoldenSword extends Weapon {
    public GoldenSword() {
        super("booster_sword");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"G", "G", "S"}, new Material[]{Material.GOLD_BLOCK, Material.STICK}, CraftingBookCategory.EQUIPMENT);

    }

}
