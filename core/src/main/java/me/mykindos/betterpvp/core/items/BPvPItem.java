package me.mykindos.betterpvp.core.items;

import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.events.items.ItemUpdateLoreEvent;
import me.mykindos.betterpvp.core.items.type.IBPvPItem;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Basic item data class, imported via database
 */
@CustomLog
@Getter
@Setter
public class BPvPItem implements IBPvPItem {
    private String namespace;
    private String key;
    private NamespacedKey namespacedKey;
    private Material material;
    private Component name;

    @Getter(AccessLevel.NONE)
    private List<Component> lore;

    private int customModelData;
    private int maxDurability;
    private boolean glowing;
    private boolean giveUUID;
    private List<NamespacedKey> recipeKeys;

    public BPvPItem(String namespace, String key, Material material, Component name, List<Component> lore, int maxDurability, int customModelData, boolean glowing, boolean uuid) {
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
        recipeKeys = new ArrayList<>();
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
     * @param itemStack the item stack to apply custom features,
     * @return the full custom itemstack
     */
    public ItemStack itemify(ItemStack itemStack) {
        return itemify(itemStack, itemStack.getItemMeta());
    }

    /**
     * @param itemStack the item stack to apply custom features,
     * @param itemMeta the item meta of the item stack to modify
     * @return the full custom itemstack
     */

    @Contract(value = "_, _ -> param1", mutates = "param1, param2")
    public ItemStack itemify(ItemStack itemStack, ItemMeta itemMeta) {
        if (!matches(itemStack)) return itemStack;
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        itemMeta.displayName(getName());
        if (!dataContainer.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            dataContainer.set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, getIdentifier());
        }
        if (getMaxDurability() >= 0) {
            Damageable damageable = (Damageable) itemMeta;
            damageable.setMaxDamage(getMaxDurability());
            if (!damageable.hasDamageValue()) {
                damageable.setDamage(0);
            }
        }
        applyLore(itemStack, itemMeta);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public String getIdentifier() {
        return this.namespacedKey.asString();
    }

    public @NotNull Component getName() {
        return this.name;
    }

    public String getSimpleName() {
        return PlainTextComponentSerializer.plainText().serialize(getName());
    }


    /**
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
            if (itemMeta.hasCustomModelData()) {
                return itemMeta.getCustomModelData() == customModelData;
            }
            return false;
        }
        return true;
    }

    /**
     * @param itemStack1 First ItemStack to campare
     * @param itemStack2 Second ItemStack to compare
     * @return true if itemStack1 is most likely the same item as itemStack2, false otherwise
     */
    public boolean compareExactItem(ItemStack itemStack1, ItemStack itemStack2) {
        if (itemStack1 == itemStack2) {
            return true;
        }
        if (itemStack1 == null || itemStack2 == null) {
            return false;
        }
        if (matches(itemStack1) && matches(itemStack2)) {
            ItemMeta itemMeta1 = itemStack1.getItemMeta();
            ItemMeta itemMeta2 = itemStack2.getItemMeta();

            PersistentDataContainer pdc1 = itemMeta1.getPersistentDataContainer();
            PersistentDataContainer pdc2 = itemMeta2.getPersistentDataContainer();
            if (isGiveUUID()) {
                if (pdc1.has(CoreNamespaceKeys.UUID_KEY) && pdc2.has(CoreNamespaceKeys.UUID_KEY)) {
                    return Objects.requireNonNull(pdc1.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING)).equals(pdc2.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING));
                }
            }

            if (itemMeta1 instanceof Damageable damageable1 && itemMeta2 instanceof Damageable damageable2) {
                if (damageable1.hasDamageValue() && damageable2.hasDamageValue()) {
                    return damageable1.getDamage() == damageable2.getDamage();
                }
            }
        }
        return false;
    }

    /**
     * Gets a single recipe with the default suffix "shaped"
     * Should only be used if the item will only have 1 shaped recipe
     *
     * @param shape a shape array of strings accepted by ShapedRecipe
     * @return a ShapedRecipe set to an instance of this item
     */
    public ShapedRecipe getShapedRecipe(String... shape) {
        return getShapedRecipe(1, shape);
    }

    /**
     * Gets a single recipe with the default suffix "shaped"
     * Should only be used if the item will only have 1 shaped recipe
     *
     * @param shape a shape array of strings accepted by ShapedRecipe
     * @return a ShapedRecipe set to an instance of this item
     */
    public ShapedRecipe getShapedRecipe(int recipeNumber, String... shape) {
        return getShapedRecipe(1, recipeNumber, shape);
    }

    /**
     * Gets a single recipe with the default suffix "shaped", accepts multiple items
     * Should only be used if the item will only have 1 shaped recipe
     *
     * @param count the number of items this recipe creates
     * @param shape a shape array of strings accepted by ShapedRecipe
     * @return a ShapedRecipe set to an instance of this item
     */
    public ShapedRecipe getShapedRecipe(int count, int recipeNumber, String... shape) {
        return getShapedRecipe(count, "shaped" + recipeNumber, shape);
    }

    /**
     * Create a shaped recipe, with a Namespacedkey key portion of 'key_' + key_suffix
     *
     * @param count      the number of items this recipe creates
     * @param key_suffix the suffix for this recipe key, default is "shaped"
     * @param shape      a shape array of strings accepted by ShapedRecipe
     * @return a ShapedRecipe set to an instance of this item
     */
    public ShapedRecipe getShapedRecipe(int count, String key_suffix, String... shape) {
        ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(namespace, key + "_" + key_suffix), getItemStack(count));
        shapedRecipe.shape(shape);
        return shapedRecipe;
    }

    /**
     * Create a shapeless recipe, with a Namespacedkey key portion of 'key_' + key_suffix
     *
     * @param count       the number of items this recipe creates
     * @param key_suffix  the suffix for this recipe key, default is "shapeless"
     * @param ingredients a list of ingredients
     * @return a ShapelessRecipe set to an instance of this item
     */
    public ShapelessRecipe getShapelessRecipe(int count, String key_suffix, ItemStack... ingredients) {
        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(namespace, key + key_suffix), getItemStack(count));
        for (ItemStack ingredient : ingredients) {
            shapelessRecipe.addIngredient(ingredient);
        }
        return shapelessRecipe;
    }

    protected void createShapedRecipe(String[] layout, Material[] materials, CraftingBookCategory category) {

        UtilItem.removeRecipe(getMaterial());

        ShapedRecipe shapedRecipe = getShapedRecipe(layout);

        // Get list of unique chars from String array
        List<Character> chars = new ArrayList<>();
        for (String s : layout) {
            for (char c : s.toCharArray()) {
                if (c == ' ') continue;
                if (!chars.contains(c)) {
                    chars.add(c);
                }
            }
        }

        // Add ingredients to recipe
        for (int i = 0; i < chars.size(); i++) {
            shapedRecipe.setIngredient(chars.get(i), materials[Math.min(i, materials.length - 1)]);
        }

        shapedRecipe.setCategory(category);

        Bukkit.addRecipe(shapedRecipe);
        recipeKeys.add(shapedRecipe.getKey());

    }

    @Override
    public List<Component> getLore(ItemMeta meta) {
        return lore;
    }

    @Contract(value = "_, _ -> param2", mutates = "param2")
    public ItemMeta applyLore(ItemStack itemStack, @NotNull ItemMeta itemMeta) {

        List<Component> newLore = new ArrayList<>(this.getLore(itemMeta));
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();

        ItemUpdateLoreEvent event = UtilServer.callEvent(new ItemUpdateLoreEvent(this, itemStack, itemMeta, newLore));

        newLore = event.getItemLore();
        if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
            newLore.add(UtilMessage.deserialize("<dark_gray>%s</dark_gray>", pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING)).decoration(TextDecoration.ITALIC, false));
        }

        itemMeta.lore(UtilItem.removeItalic(newLore));
        return itemMeta;
    }
}
