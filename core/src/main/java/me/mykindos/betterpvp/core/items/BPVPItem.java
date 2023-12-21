package me.mykindos.betterpvp.core.items;

import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Basic item data class, imported via database
 */
@Data
public class BPVPItem {

    private String namespace;
    private String key;
    private final Material material;
    private final Component name;
    private final List<Component> lore;
    private int customModelData;
    private final boolean glowing;
    private final boolean giveUUID;

    private ShapedRecipe[] shapedRecipes;
    private ShapelessRecipe[] shapelessRecipes;
    //todo add all recipes

    BPVPItem () {

    }

    @Deprecated
    public BPVPItem(Material material, Component name, List<Component> lore, int customModelData, boolean glowing, boolean uuid) {
        this("betterpvp", PlainTextComponentSerializer.plainText().serialize(name), material, name, lore, customModelData, glowing, uuid);
    }

    public BPVPItem(String namespace, String key, Material material, Component name, List<Component> lore, int customModelData, boolean glowing, boolean uuid) {
        this.namespace = namespace;
        this.key = key;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.glowing = glowing;
        this.giveUUID = uuid;
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
