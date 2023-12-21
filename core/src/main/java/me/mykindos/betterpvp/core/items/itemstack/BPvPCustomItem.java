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
     * @param shape a shape function accepted by ShapedRecipe
     * @see ShapedRecipe#shape(String...)
     * @return The ShapedRecipe, which can be safely discarded
     */
    protected void newShapedRecipe(String... shape) {
        this.shapedRecipe = new ShapedRecipe(new NamespacedKey(namespacedKey.namespace(), namespacedKey.value() + "_shaped"), getItemStack());
        shapedRecipe.shape(shape);
    }

    protected void setShapedRecipe(ShapedRecipe shapedRecipe) {
        Bukkit.addRecipe(shapedRecipe);
        this.shapedRecipe = shapedRecipe;
    }



    protected ShapelessRecipe setShapelessRecipe(List<ItemStack> ingredients) {
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(namespacedKey.namespace(), namespacedKey.value() + "_shapeless"), getItemStack());
        for (ItemStack ingredient : ingredients) {
            shapelessRecipe.addIngredient(ingredient);
        }
        Bukkit.addRecipe(shapelessRecipe);
        this.shapelessRecipe = shapelessRecipe;
        return shapelessRecipe;
    }


}
