package me.mykindos.betterpvp.clans.weapons.impl.doors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.items.BPvPItem;
import org.bukkit.Material;
import org.bukkit.inventory.recipe.CraftingBookCategory;

@Singleton
public class IronTrapdoor extends Weapon {

    @Inject
    public IronTrapdoor(Clans clans) {
        super(clans, "iron_trapdoor");
    }

    @Override
    public void loadWeapon(BPvPItem item) {

        super.loadWeapon(item);

        this.createShapelessRecipe(1, "_acacia", CraftingBookCategory.BUILDING, Material.ACACIA_TRAPDOOR);
        this.createShapelessRecipe(1, "_birch", CraftingBookCategory.BUILDING, Material.BIRCH_TRAPDOOR);
        this.createShapelessRecipe(1, "_cherry", CraftingBookCategory.BUILDING, Material.CHERRY_TRAPDOOR);
        this.createShapelessRecipe(1, "_dark_oak", CraftingBookCategory.BUILDING, Material.DARK_OAK_TRAPDOOR);
        this.createShapelessRecipe(1, "_jungle", CraftingBookCategory.BUILDING, Material.JUNGLE_TRAPDOOR);
        this.createShapelessRecipe(1, "_mangrove", CraftingBookCategory.BUILDING, Material.MANGROVE_TRAPDOOR);
        this.createShapelessRecipe(1, "_oak", CraftingBookCategory.BUILDING, Material.OAK_TRAPDOOR);
        this.createShapelessRecipe(1, "_bamboo", CraftingBookCategory.BUILDING, Material.BAMBOO_TRAPDOOR);
        this.createShapelessRecipe(1, "_crimson", CraftingBookCategory.BUILDING, Material.CRIMSON_TRAPDOOR);
        this.createShapelessRecipe(1, "_warped", CraftingBookCategory.BUILDING, Material.WARPED_TRAPDOOR);

    }

}