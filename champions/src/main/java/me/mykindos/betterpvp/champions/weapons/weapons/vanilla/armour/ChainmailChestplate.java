package me.mykindos.betterpvp.champions.weapons.weapons.vanilla.armour;

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
public class ChainmailChestplate extends Weapon {
    public ChainmailChestplate() {
        super("ranger_vest");
    }

    @Override
    public void loadWeapon(BPVPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"E E", "EEE", "EEE"}, new Material[]{Material.EMERALD}, CraftingBookCategory.EQUIPMENT);
    }

}
