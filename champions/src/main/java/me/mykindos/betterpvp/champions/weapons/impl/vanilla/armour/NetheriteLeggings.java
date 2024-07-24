package me.mykindos.betterpvp.champions.weapons.impl.vanilla.armour;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NetheriteLeggings extends Weapon {
    @Inject
    public NetheriteLeggings(Champions champions) {
        super(champions, "warlock_leggings");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"EEE", "E E", "E E"}, new Material[]{Material.NETHERITE_INGOT}, CraftingBookCategory.EQUIPMENT);
    }

}
