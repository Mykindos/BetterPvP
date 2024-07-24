package me.mykindos.betterpvp.champions.weapons.impl.vanilla.armour;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

@Singleton
public class NetheriteHelmet extends Weapon {
    @Inject
    public NetheriteHelmet(Champions champions) {
        super(champions, "warlock_helmet");
    }

    @Override
    public void loadWeapon(BPvPItem item) {
        super.loadWeapon(item);

        createShapedRecipe(new String[]{"EEE", "E E"}, new Material[]{Material.NETHERITE_INGOT}, CraftingBookCategory.EQUIPMENT);
    }

}
