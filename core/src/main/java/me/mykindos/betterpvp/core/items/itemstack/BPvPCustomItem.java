package me.mykindos.betterpvp.core.items.itemstack;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
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
    private ShapedRecipe shapedRecipe;
    private ShapelessRecipe shapelessRecipe;

    private final int model;
    private final Material material;

    Component name;

    private final NamespacedKey namespacedKey;

    BPvPPlugin plugin;

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
        item.getItemMeta().setCustomModelData(model);
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
     * @param ingredients a KeyValue<\Character, ItemStack>, which is a list of characters used in shape and their respective ItemStack
     * @return The ShapedRecipe, which can be safely discarded
     */
    protected ShapedRecipe setShapedRecipe(String[] shape, KeyValue<Character, ItemStack>[] ingredients) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(namespacedKey.namespace(), namespacedKey.value() + "_shaped"), getItemStack());
        shapedRecipe.shape(shape);
        for (KeyValue<Character, ItemStack> ingredient : ingredients) {
            shapedRecipe.setIngredient(ingredient.getKey(), ingredient.getValue());
        }
        Bukkit.addRecipe(shapedRecipe);
        this.shapedRecipe = shapedRecipe;
        return shapedRecipe;
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
