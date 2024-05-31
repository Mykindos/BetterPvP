package me.mykindos.betterpvp.champions.weapons.impl.vanilla.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Singleton;

@Singleton
public class DiamondAxe extends Weapon {
    @Inject
    public DiamondAxe(Champions champions) {
        super(champions, "power_axe");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{" DD", " SD", " S "}, new Material[]{Material.DIAMOND_BLOCK, Material.STICK}, CraftingBookCategory.EQUIPMENT);

    }

}
