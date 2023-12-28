package me.mykindos.betterpvp.core.items;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Basic item data class, imported via database
 */
@Slf4j
@Getter
@Setter
public class BPVPItem {
    private String namespace;
    private String key;
    private NamespacedKey namespacedKey;
    private Material material;
    private Component name;
    private List<Component> lore;
    private int customModelData;
    private int maxDurability;
    private boolean glowing;
    private boolean giveUUID;

    //not sure if I need to save recipes
    protected ShapedRecipe[] shapedRecipes = new ShapedRecipe[1];
    private ShapelessRecipe[] shapelessRecipes = new ShapelessRecipe[1];
    //todo add all recipes

    public BPVPItem(String namespace, String key, Material material, Component name, List<Component> lore, int maxDurability, int customModelData, boolean glowing, boolean uuid) {
        this.namespace = namespace;
        this.key = key;
        this.namespacedKey = new NamespacedKey(namespace, key);
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.maxDurability = maxDurability;
        this.glowing = glowing;
        this.giveUUID = uuid;
    }


    public ItemStack getItemStack() {
        return getItemStack(1);
    }

    public ItemStack getItemStack(int count) {
        ItemStack item = new ItemStack(material, count);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        item.setItemMeta(itemMeta);
        return itemify(item);
    }

    /**
     *
     * @param itemStack the item stack to apply custom features,
     * @return the full custom itemstack
     */
    public ItemStack itemify(ItemStack itemStack) {
        if (!matches(itemStack)) return itemStack;
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        itemMeta.displayName(getName());
        if (isGiveUUID()) {
            if (!dataContainer.has(CoreNamespaceKeys.UUID_KEY)) {
                dataContainer.set(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
        }
        if (!dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            dataContainer.set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, getIdentifier());
        }
        if (getMaxDurability() >= 0) {
            log.info("dura apply");
            if (!dataContainer.has(CoreNamespaceKeys.DURABILITY_KEY)) {
                dataContainer.set(CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER, getMaxDurability());
                log.info("add dura key");
                applyLore(itemMeta, getMaxDurability());
            } else {
                int durability = dataContainer.get(CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER);
                log.info("add curr dura");
                applyLore(itemMeta, durability);
            }
        } else {
            log.info("standard apply");
            applyLore(itemMeta);
        }
        itemStack.setItemMeta(itemMeta);
        log.info("lore" + itemStack.lore());
        log.warn("getLore " + getLore());
        return itemStack;
    }

    public String getIdentifier() {
        return this.namespacedKey.asString();
    }

    public @NotNull Component getName() {
        return this.name;
    }

    /**
     *
     * @param itemStack the item stack to compare to
     * @return true if the itemstack is an instance of this item
     */
    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if (dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            return Objects.requireNonNull(dataContainer.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING)).equalsIgnoreCase(getIdentifier());
        }
        if (customModelData != 0) {

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
        this.shapedRecipes[0] = getShapedRecipe(count, "_shaped", shape);
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
        this.shapelessRecipes[0] = getShapelessRecipe(count, "_shapeless", ingredients);
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

    /**]
     * Damage an item of this type
     * @param player the player damaging
     * @param itemStack the ItemStack to damage
     * @param damage the damage the ItemStack should take
     * @return the damaged ItemStack
     */
    public ItemStack damageItem (Player player, ItemStack itemStack, int damage) {
        if (getMaxDurability() < 0) return itemStack;
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if (!dataContainer.has(CoreNamespaceKeys.DURABILITY_KEY)) {
            log.warn("Itemstack of type: " + getIdentifier() + " is not initialized, but took damage. " +  itemStack.toString());
            return itemStack;
        }

        int newDurability = dataContainer.get(CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER) - damage;
        dataContainer.set(CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER, newDurability);
        if (newDurability < 0) {
            player.getInventory().removeItem(itemStack);
        }
        applyLore(itemMeta, newDurability);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private ItemMeta applyLore(ItemMeta itemMeta) {
        itemMeta.lore(UtilItem.removeItalic(getLore()));
        log.error("LORE APPLY" + itemMeta.lore().toString());
        return itemMeta;
    }

    private ItemMeta applyLore(ItemMeta itemMeta, int durability) {

        List<Component> newLore = UtilItem.removeItalic(getLore());
        newLore.add(0, UtilMessage.deserialize("<grey>Durability: %s</grey>", durability));
        itemMeta.lore(newLore);
        log.info("Dura Lore update: " + durability + " " + newLore);
        log.warn( "lore" + itemMeta.lore());
        return itemMeta;
    }
}
