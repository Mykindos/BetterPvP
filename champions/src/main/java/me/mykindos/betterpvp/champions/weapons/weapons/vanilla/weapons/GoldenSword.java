package me.mykindos.betterpvp.champions.weapons.weapons.vanilla.weapons;

import me.mykindos.betterpvp.champions.weapons.Weapon;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class GoldenSword extends Weapon {
    public GoldenSword() {
        super("booster_sword");
    }

    @Override
    public void loadWeapon(BPVPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"G", "G", "S"}, new Material[]{Material.GOLD_BLOCK, Material.STICK}, CraftingBookCategory.EQUIPMENT);

    }

}
