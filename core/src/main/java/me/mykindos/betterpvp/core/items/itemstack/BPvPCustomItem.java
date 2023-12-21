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

@Getter
public abstract class BPvPCustomItem {
    protected ShapedRecipe shapedRecipe;
    protected ShapelessRecipe shapelessRecipe;

    protected final int model;
    protected final Material material;

    protected Component name;

    protected final NamespacedKey namespacedKey;

    protected BPvPCustomItem(NamespacedKey namespacedKey, Component name, Material material, int model) {
        this.namespacedKey = namespacedKey;
        this.material = material;
        this.name = name;
        this.model = model;
    }

    public ItemStack getItemStack() {
        return getItemStack(1);
    }

    public ItemStack getItemStack(int count) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(model);
        item.setItemMeta(itemMeta);
        return item;
    }
    public @NotNull Component getName() {
        return this.name;
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) return false;
        if (model != 0) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                if (itemMeta.hasCustomModelData()) {
                    return itemMeta.getCustomModelData() == model;
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
        this.shapedRecipe = getShapedRecipe(count, "shaped", shape);
    }

    //todo expose/implement some way to have more than 1 shaped recipe for an item
    private ShapedRecipe getShapedRecipe(int count, String key_suffix, String... shape) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(namespacedKey.namespace(), namespacedKey.value() + key_suffix), getItemStack(count));
        shapedRecipe.shape(shape);
        return shapedRecipe;
    }

    protected void setShapelessRecipe(ItemStack... ingredients) {
        setShapelessRecipe(1, ingredients);
    }

    protected void setShapelessRecipe(int count, ItemStack... ingredients) {
        this.shapelessRecipe = getShapelessRecipe(count, "shapeless", ingredients);
        Bukkit.addRecipe(shapelessRecipe);
    }

    //todo expose/implement some way to have more than 1 shapeless recipe for an item
    private ShapelessRecipe getShapelessRecipe(int count, String key_suffix, ItemStack... ingredients) {
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(namespacedKey.namespace(), namespacedKey.value() + key_suffix), getItemStack(count));
        for (ItemStack ingredient : ingredients) {
            shapelessRecipe.addIngredient(ingredient);
        }
        return shapelessRecipe;
    }


}
