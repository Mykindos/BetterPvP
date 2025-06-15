package me.mykindos.betterpvp.clans.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.item.cannon.CannonItem;
import me.mykindos.betterpvp.clans.item.cannon.CannonballItem;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.model.VanillaItem;
import me.mykindos.betterpvp.core.item.renderer.ItemLoreRenderer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
public class ClansItemBoostrap {

    private final Clans clans;
    private final ItemRegistry itemRegistry;

    @Inject
    private ClansItemBoostrap(Clans clans, ItemRegistry itemRegistry) {
        this.clans = clans;
        this.itemRegistry = itemRegistry;
    }

    @Inject
    private void registerItems(CannonItem cannonItem, CannonballItem cannonballItem) {
        final VanillaItem waterBlock = new VanillaItem("Water Block", ItemStack.of(Material.LAPIS_BLOCK), ItemRarity.UNCOMMON);
        itemRegistry.registerFallbackItem(new NamespacedKey(clans, "water_block"), Material.LAPIS_BLOCK, waterBlock);

        itemRegistry.registerItem(new NamespacedKey(clans, "cannon"), cannonItem);
        itemRegistry.registerItem(new NamespacedKey(clans, "cannonball"), cannonballItem);
    }

    @Inject
    private void registerDoorRecipes() {
        // todo: add door recipes
//        this.createShapelessRecipe(1, "_acacia", CraftingBookCategory.BUILDING, Material.ACACIA_DOOR);
//        this.createShapelessRecipe(1, "_birch", CraftingBookCategory.BUILDING, Material.BIRCH_DOOR);
//        this.createShapelessRecipe(1, "_cherry", CraftingBookCategory.BUILDING, Material.CHERRY_DOOR);
//        this.createShapelessRecipe(1, "_dark_oak", CraftingBookCategory.BUILDING, Material.DARK_OAK_DOOR);
//        this.createShapelessRecipe(1, "_jungle", CraftingBookCategory.BUILDING, Material.JUNGLE_DOOR);
//        this.createShapelessRecipe(1, "_spruce", CraftingBookCategory.BUILDING, Material.SPRUCE_DOOR);
//        this.createShapelessRecipe(1, "_mangrove", CraftingBookCategory.BUILDING, Material.MANGROVE_DOOR);
//        this.createShapelessRecipe(1, "_oak", CraftingBookCategory.BUILDING, Material.OAK_DOOR);
//        this.createShapelessRecipe(1, "_bamboo", CraftingBookCategory.BUILDING, Material.BAMBOO_DOOR);
//        this.createShapelessRecipe(1, "_crimson", CraftingBookCategory.BUILDING, Material.CRIMSON_DOOR);
//        this.createShapelessRecipe(1, "_warped", CraftingBookCategory.BUILDING, Material.WARPED_DOOR);
    }

    @Inject
    private void registerTrapdoorRecipes() {
        // todo: add trapdoor recipes
//        this.createShapelessRecipe(1, "_acacia", CraftingBookCategory.BUILDING, Material.ACACIA_TRAPDOOR);
//        this.createShapelessRecipe(1, "_birch", CraftingBookCategory.BUILDING, Material.BIRCH_TRAPDOOR);
//        this.createShapelessRecipe(1, "_cherry", CraftingBookCategory.BUILDING, Material.CHERRY_TRAPDOOR);
//        this.createShapelessRecipe(1, "_dark_oak", CraftingBookCategory.BUILDING, Material.DARK_OAK_TRAPDOOR);
//        this.createShapelessRecipe(1, "_jungle", CraftingBookCategory.BUILDING, Material.JUNGLE_TRAPDOOR);
//        this.createShapelessRecipe(1, "_spruce", CraftingBookCategory.BUILDING, Material.SPRUCE_TRAPDOOR);
//        this.createShapelessRecipe(1, "_mangrove", CraftingBookCategory.BUILDING, Material.MANGROVE_TRAPDOOR);
//        this.createShapelessRecipe(1, "_oak", CraftingBookCategory.BUILDING, Material.OAK_TRAPDOOR);
//        this.createShapelessRecipe(1, "_bamboo", CraftingBookCategory.BUILDING, Material.BAMBOO_TRAPDOOR);
//        this.createShapelessRecipe(1, "_crimson", CraftingBookCategory.BUILDING, Material.CRIMSON_TRAPDOOR);
//        this.createShapelessRecipe(1, "_warped", CraftingBookCategory.BUILDING, Material.WARPED_TRAPDOOR);
    }

}
