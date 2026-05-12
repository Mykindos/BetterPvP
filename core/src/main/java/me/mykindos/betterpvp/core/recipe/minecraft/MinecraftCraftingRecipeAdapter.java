package me.mykindos.betterpvp.core.recipe.minecraft;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for working with Minecraft's built-in recipes.
 * Handles finding, matching, and converting Minecraft recipes to our custom recipe format.
 */
@CustomLog
@Singleton
public class MinecraftCraftingRecipeAdapter {

    private final Provider<ItemFactory> itemFactory;
    private final Provider<CraftingRecipeRegistry> registry;
    private final Set<Recipe> disabledRecipes = new HashSet<>();

    @Inject
    private MinecraftCraftingRecipeAdapter(Provider<ItemFactory> itemFactory, Provider<CraftingRecipeRegistry> registry) {
        this.itemFactory = itemFactory;
        this.registry = registry;
    }

    public Map<NamespacedKey, CraftingRecipe> getRecipes() {
        Map<NamespacedKey, CraftingRecipe> recipes = new HashMap<>();
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof org.bukkit.inventory.CraftingRecipe craftingRecipe && !disabledRecipes.contains(craftingRecipe)) {
                CraftingRecipe convertedRecipe = convertToCustomRecipe(craftingRecipe);
                if (convertedRecipe != null) {
                    recipes.put(craftingRecipe.getKey(), convertedRecipe);
                }

            }
        });
        return recipes;
    }

    /**
     * Converts a Minecraft recipe to our recipe format.
     * 
     * @param minecraftRecipe The Minecraft recipe to convert
     * @return Our recipe format
     */
    public CraftingRecipe convertToCustomRecipe(org.bukkit.inventory.Recipe minecraftRecipe) {
        ItemStack resultStack = minecraftRecipe.getResult();
        if (minecraftRecipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
            return convertShapedRecipe(shapedRecipe, resultStack);
        } else if (minecraftRecipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
            return convertShapelessRecipe(shapelessRecipe, resultStack);
        } else {
            return null; // Unsupported recipe type
        }
    }

    public Map<NamespacedKey, CraftingRecipe> disableRecipesFor(Material material) {
        final List<Recipe> mcRecipes = Bukkit.getRecipesFor(new ItemStack(material));
        final Map<NamespacedKey, CraftingRecipe> recipes = new HashMap<>();
        disabledRecipes.addAll(mcRecipes);

        final CraftingRecipeRegistry recipeRegistry = registry.get();
        for (Recipe recipe : mcRecipes) {
            if (recipe instanceof org.bukkit.inventory.CraftingRecipe craftingRecipe) {
                recipeRegistry.clearRecipe(craftingRecipe.getKey());
                recipes.put(craftingRecipe.getKey(), convertToCustomRecipe(craftingRecipe));
            }
        }
        return recipes;
    }
    
    /**
     * Converts a Minecraft shaped recipe to our recipe format.
     * 
     * @param shapedRecipe The Minecraft shaped recipe to convert
     * @param result Our result ItemInstance
     * @return Our shaped recipe format
     */
    private ShapedCraftingRecipe convertShapedRecipe(org.bukkit.inventory.ShapedRecipe shapedRecipe, ItemStack result) {
        String[] shape = shapedRecipe.getShape();
        Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();

        final int amount = result.getAmount();
        ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(
                itemFactory.get().getFallbackItem(result),
                instance -> instance.getItemStack().setAmount(amount),
                shape,
                itemFactory.get());

        for (Map.Entry<Character, RecipeChoice> entry : choiceMap.entrySet()) {
            RecipeIngredient ingredient = ingredientFromChoice(entry.getValue());
            if (ingredient != null) {
                builder.setIngredient(entry.getKey(), ingredient);
            }
        }

        return builder.build();
    }
    
    /**
     * Converts a Minecraft shapeless recipe to our recipe format.
     * 
     * @param shapelessRecipe The Minecraft shapeless recipe to convert
     * @param result Our result ItemInstance
     * @return Our shapeless recipe format
     */
    private ShapelessCraftingRecipe convertShapelessRecipe(org.bukkit.inventory.ShapelessRecipe shapelessRecipe, ItemStack result) {
        Map<Integer, RecipeIngredient> ingredientMap = new HashMap<>();
        int index = 0;
        for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
            RecipeIngredient ingredient = ingredientFromChoice(choice);
            if (ingredient != null) {
                ingredientMap.put(index++, ingredient);
            }
        }

        final int amount = result.getAmount();
        return new ShapelessCraftingRecipe(
                itemFactory.get().getFallbackItem(result),
                instance -> instance.getItemStack().setAmount(amount),
                ingredientMap,
                itemFactory.get(),
                false);
    }

    /**
     * Flattens a Bukkit {@link RecipeChoice} (which represents a vanilla item tag or exact-stack list)
     * into a {@link RecipeIngredient} that accepts every {@link BaseItem} in the choice.
     * <p>
     * Previously this code took {@code choices.getFirst()} and discarded every alternative, which
     * silently broke any recipe whose ingredient was a tag (chest planks, torch coals, etc.).
     */
    private RecipeIngredient ingredientFromChoice(RecipeChoice choice) {
        Set<BaseItem> baseItems = new LinkedHashSet<>();
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            for (Material material : materialChoice.getChoices()) {
                itemFactory.get().fromItemStack(new ItemStack(material))
                        .ifPresent(instance -> baseItems.add(instance.getBaseItem()));
            }
        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            for (ItemStack stack : exactChoice.getChoices()) {
                itemFactory.get().fromItemStack(stack)
                        .ifPresent(instance -> baseItems.add(instance.getBaseItem()));
            }
        } else {
            return null;
        }

        if (baseItems.isEmpty()) {
            return null;
        }
        return new RecipeIngredient(baseItems, 1);
    }

    /**
     * Reads {@code items/recipes.yml} from the given plugin's data folder and disables any recipes
     * listed there.  Two sections are supported:
     * <ul>
     *   <li>{@code disabled.materials} – list of {@link Material} enum names; every vanilla recipe
     *       that produces the material is disabled via {@link #disableRecipesFor(Material)}.</li>
     *   <li>{@code disabled.keys} – list of full {@link NamespacedKey} strings (e.g.
     *       {@code minecraft:oak_boat}); each recipe is removed directly from the registry.</li>
     * </ul>
     * If the plugin does not ship {@code configs/items/recipes.yml} the method returns silently.
     *
     * @param plugin the plugin whose {@code items/recipes.yml} should be processed
     */
    public void disableRecipesFromConfig(BPvPPlugin plugin) {
        // Only proceed if the plugin ships this config file
        if (plugin.getResource("configs/items/recipes.yml") == null) {
            return;
        }

        final ExtendedYamlConfiguration config = plugin.getConfig("items/recipes");
        final CraftingRecipeRegistry recipeRegistry = registry.get();

        // --- material-based disabling ---
        List<String> materials = config.getStringList("disabled.materials");
        for (String materialName : materials) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                log.warn("items/recipes.yml [{}]: unknown material '{}', skipping", plugin.getName(), materialName).submit();
                continue;
            }
            disableRecipesFor(material);
        }

        // --- key-based disabling ---
        List<String> keys = config.getStringList("disabled.keys");
        for (String keyStr : keys) {
            NamespacedKey key = NamespacedKey.fromString(keyStr);
            if (key == null) {
                log.warn("items/recipes.yml [{}]: invalid NamespacedKey '{}', skipping", plugin.getName(), keyStr).submit();
                continue;
            }
            recipeRegistry.clearRecipe(key);
        }

        plugin.saveConfig();
    }

    public void registerDefaults(Map<NamespacedKey, CraftingRecipe> craftingRecipes) {
        log.info("Registering default Minecraft crafting recipes").submit();

        long start = System.nanoTime();

        craftingRecipes.putAll(getRecipes());

        long elapsed = System.nanoTime() - start;
        double ms = elapsed / 1_000_000.0;

        log.info("Registered {} default Minecraft crafting recipes in {} ms",
                craftingRecipes.size(), String.format("%.2f", ms)).submit();
    }

}