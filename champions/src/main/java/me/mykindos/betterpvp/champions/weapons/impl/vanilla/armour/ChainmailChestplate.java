package me.mykindos.betterpvp.champions.weapons.impl.vanilla.armour;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

@Singleton
public class ChainmailChestplate extends Weapon {
    @Inject
    public ChainmailChestplate(Champions champions) {
        super(champions, "ranger_vest");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"E E", "EEE", "EEE"}, new Material[]{Material.EMERALD}, CraftingBookCategory.EQUIPMENT);
    }

}
