package me.mykindos.betterpvp.champions.weapons.impl.vanilla.weapons;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import com.google.inject.Singleton;

@Singleton
public class GoldenSword extends Weapon {
    @Inject
    public GoldenSword(Champions champions) {
        super(champions, "booster_sword");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"G", "G", "S"}, new Material[]{Material.GOLD_BLOCK, Material.STICK}, CraftingBookCategory.EQUIPMENT);

    }

}
