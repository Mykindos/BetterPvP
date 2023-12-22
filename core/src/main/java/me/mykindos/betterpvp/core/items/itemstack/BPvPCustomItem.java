package me.mykindos.betterpvp.core.items.itemstack;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public abstract class BPvPCustomItem {
    private String namespace;
    private String key;
    private final Material material;
    private final Component name;
    private final List<Component> lore;
    private int customModelData;

    //not sure if I need to save recipes
    private ShapedRecipe[] shapedRecipes = new ShapedRecipe[1];
    private ShapelessRecipe[] shapelessRecipes = new ShapelessRecipe[1];
    //todo add all recipes

    protected BPvPCustomItem(String namespace, String key, Material material, Component name, List<Component> lore, int customModelData) {
        this.namespace = namespace;
        this.key = key;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
    }

    public ItemStack getItemStack() {
        return getItemStack(1);
    }

    public ItemStack getItemStack(int count) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        item.setItemMeta(itemMeta);
        return item;
    }
    public @NotNull Component getName() {
        return this.name;
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) return false;
        if (customModelData != 0) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                if (itemMeta.hasCustomModelData()) {
                    return itemMeta.getCustomModelData() == customModelData;
                }
            }
        }
        return true;
    }

    /**
     *
     * @param shape a shape array of strings accepted by ShapedRecipe
     * @see ShapedRecipe#shape(String...)
     */
    protected void setShapedRecipe(String... shape) {
        setShapedRecipe(1, shape);
    }

    /**
     *
     * @param count number of items to return as apart of the recipe
     * @param shape a shape array of strings accepted by ShapedRecipe
     * @see ShapedRecipe#shape(String...)
     */
    protected void setShapedRecipe(int count, String... shape) {
        this.shapedRecipes[0] = getShapedRecipe(count, "shaped", shape);
    }

    //todo expose/implement some way to have more than 1 shaped recipe for an item
    private ShapedRecipe getShapedRecipe(int count, String key_suffix, String... shape) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(namespace, key + key_suffix), getItemStack(count));
        shapedRecipe.shape(shape);
        return shapedRecipe;
    }

    protected void setShapelessRecipe(ItemStack... ingredients) {
        setShapelessRecipe(1, ingredients);
    }

    protected void setShapelessRecipe(int count, ItemStack... ingredients) {
        this.shapelessRecipes[0] = getShapelessRecipe(count, "shapeless", ingredients);
        Bukkit.addRecipe(shapelessRecipes[0]);
    }

    //todo expose/implement some way to have more than 1 shapeless recipe for an item
    private ShapelessRecipe getShapelessRecipe(int count, String key_suffix, ItemStack... ingredients) {
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(namespace, key + key_suffix), getItemStack(count));
        for (ItemStack ingredient : ingredients) {
            shapelessRecipe.addIngredient(ingredient);
        }
        return shapelessRecipe;
    }


}
