package me.mykindos.betterpvp.champions.weapons.impl.vanilla.armour;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChainmailLeggings extends Weapon {
    @Inject
    public ChainmailLeggings(Champions champions) {
        super(champions, "ranger_leggings");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"EEE", "E E", "E E"}, new Material[]{Material.EMERALD}, CraftingBookCategory.EQUIPMENT);
    }

}
