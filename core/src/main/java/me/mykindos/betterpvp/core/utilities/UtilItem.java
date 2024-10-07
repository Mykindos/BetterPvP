package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilItem {

    public static ItemStack convertType(@NotNull ItemStack itemStackIn, @NotNull Material to, @Nullable Integer toModel) {
        ItemStack itemStack = itemStackIn.clone();
        final Material from = itemStack.getType();
        if (itemStack.getItemMeta() != null) {
            final ItemMeta metaCopy = itemStack.getItemMeta().clone();
            itemStack.setType(to);
            final ItemMeta meta = itemStack.getItemMeta();
            meta.setCustomModelData(toModel);

            if (!metaCopy.hasDisplayName()) {
                final String key = from.translationKey();
                final Component name = Component.translatable(key, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
                meta.displayName(name);
            }

            // Durability
            if (metaCopy instanceof Damageable oldItem && meta instanceof Damageable newItem) {
                final int oldMaxDurability =from.getMaxDurability();
                final int oldDurability = oldItem.getDamage();
                final int newMaxDurability = to.getMaxDurability();
                final int scaledDurability = (int) Math.round(((double) oldDurability / oldMaxDurability) * newMaxDurability);
                newItem.setDamage(scaledDurability);
            }

            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public static boolean isCosmeticShield(ItemStack item) {
        return item.getType() == Material.SHIELD && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN)
                && Boolean.TRUE.equals(item.getItemMeta().getPersistentDataContainer().get(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN));
    }

    public static ItemStack createCosmeticShield(int modelData) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        final ItemMeta meta = shield.getItemMeta();
        meta.setCustomModelData(modelData);
        meta.getPersistentDataContainer().set(CoreNamespaceKeys.UNDROPPABLE_KEY, PersistentDataType.BOOLEAN, true);
        shield.setItemMeta(meta);
        return shield;
    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
        if (lore != null) {
            return setItemNameAndLore(item, name, Arrays.asList(lore));
        }

        return setItemNameAndLore(item, name, new ArrayList<>());

    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, String name, List<String> lore) {
        ItemMeta im = item.getItemMeta();
        im.displayName(Component.text(name));
        if (lore != null) {
            List<Component> components = new ArrayList<>();
            for (String loreEntry : lore) {
                components.add(Component.text(loreEntry));
            }
            im.lore(components);
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(im);
        return item;
    }

    /**
     * Updates an ItemStack, giving it a custom name and lore
     *
     * @param item ItemStack to modify
     * @param name Name to give the ItemStack
     * @param lore Lore to give the ItemStack
     * @return Returns the ItemStack with the newly adjusted name and lore
     */
    public static ItemStack setItemNameAndLore(ItemStack item, Component name, List<Component> lore) {
        ItemMeta im = item.getItemMeta();
        im.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (lore != null) {
            im.lore(removeItalic(lore));
        }

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS);

        item.setItemMeta(im);
        return item;
    }

    /**
     * Removes attributes from an ItemStack (e.g. the +7 damage that is visible
     * on a diamond sword under the lore)
     *
     * @param item ItemStack to update
     * @return Returns an itemstack without its attributes
     */
    public static ItemStack removeAttributes(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(itemMeta);


        return item;
    }

    /**
     * Removes Enchants from an ItemStack (e.g. the `Sharpness iV` that is visible
     * on a diamond sword's lore)
     *
     * @param item ItemStack to update
     * @return Returns an itemstack without its attributes
     */
    public static ItemStack removeEnchants(ItemStack item) {
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            item.removeEnchantment(entry.getKey());
        }


        return item;
    }

    /**
     * Add the 'enchanted' glowing effect to any ItemStack
     *
     * @param meta Item to update
     */
    public static void addGlow(ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
    }

    /**
     * Check if a Material is a type of sword
     *
     * @param item Material to check
     * @return Returns true if the Material is a type of sword
     */
    public static boolean isSword(ItemStack item) {
        return item.getType().name().contains("_SWORD");
    }

    public static boolean isSimilar(ItemStack item, ItemStack other) {
        return (item.getType() == Material.AIR && other.getType() == Material.AIR) || (other.isSimilar(item));
    }

    /**
     * Check if a Material is a type of axe
     *
     * @param axeType Material to check
     * @return Returns true if the Material is a type of axe
     */
    public static boolean isAxe(ItemStack axeType) {
        return axeType.getType().name().contains("_AXE");
    }

    /**
     * Check if a Material is a type of pickaxe
     *
     * @param pickType Material to check
     * @return Returns true if the Material is a type of pickaxe
     */
    public static boolean isPickaxe(Material pickType) {
        return pickType.name().contains("_PICKAXE");
    }

    public static boolean isShovel(Material shovelType) {
        return shovelType.name().contains("_SHOVEL");
    }

    /**
     * Check if a Material is a type of hoe
     *
     * @param hoeType Material to check
     * @return Returns true if the Material is a type of hoe
     */
    public static boolean isHoe(Material hoeType) {
        return hoeType.name().contains("_HOE");
    }

    public static boolean isTool(ItemStack item) {
        return isPickaxe(item.getType()) || isHoe(item.getType()) || isShovel(item.getType()) || isAxe(item);
    }

    public static boolean isWeapon(ItemStack itemStack) {
        return isSword(itemStack) || isAxe(itemStack) || isRanged(itemStack);
    }

    public static boolean isArmour(Material material) {
        return material.name().contains("_CAP") || material.name().contains("_HELMET") || material.name().contains("_CHESTPLATE")
                || material.name().contains("_LEGGINGS") || material.name().contains("_BOOTS");
    }

    /**
     * Check if a Material is a type of ranged weapon
     *
     * @param wep Material to check
     * @return Returns true if the Material is a type of ranged weapon
     */
    public static boolean isRanged(ItemStack wep) {
        return (wep.getType() == Material.BOW || wep.getType() == Material.CROSSBOW);
    }

    public static void insert(Player player, ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(stack);
            } else {
                player.getWorld().dropItem(player.getLocation(), stack);
            }
        }
    }

    /**
     *
     * @param itemMeta the itemMeta to check
     * @param defaultDurability the default durability of the item
     * @return the current durability of the item
     */
    public static int getOrSaveCustomDurability(ItemMeta itemMeta, int defaultDurability) {
        return getOrSavePersistentData(itemMeta, CoreNamespaceKeys.DURABILITY_KEY, PersistentDataType.INTEGER, defaultDurability);
    }

    public static <T, Z> Z getOrSavePersistentData(ItemMeta itemMeta, NamespacedKey namespacedKey, PersistentDataType<T, Z> type, Z defaultValue) {
        PersistentDataContainer dataContainer = itemMeta.getPersistentDataContainer();
        if (!dataContainer.has(namespacedKey, type)) {
            dataContainer.set(namespacedKey, type, defaultValue);
            return defaultValue;
        }
        return dataContainer.getOrDefault(namespacedKey, type, defaultValue);
    }

    public static int indexOf(String matchingText, List<Component> components) {
        for (int i = 0; i < components.size(); i++) {
            String componentText = PlainTextComponentSerializer.plainText().serialize(components.get(i));
            if (componentText.equalsIgnoreCase(matchingText)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Create a simple ItemStack with a specific model
     * @param material The material
     * @param customModelData The model ID
     * @return The ItemStack
     */
    public static ItemStack createItemStack(Material material, int amount, int customModelData) {
        var itemStack = new ItemStack(material, amount);
        if(customModelData > 0) {
            var itemMeta = itemStack.getItemMeta();
            itemMeta.setCustomModelData(customModelData);
            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public static ItemStack createItemStack(Material material, int customModelData) {
        return createItemStack(material, 1, customModelData);
    }

    // Create a function to remove TextDecoration.ITALIC from List<Component>
    public static List<Component> removeItalic(List<Component> components) {
        List<Component> newComponents = new ArrayList<>();
        for (Component component : components) {
            newComponents.add(component.decoration(TextDecoration.ITALIC, false));
        }
        return newComponents;
    }

    public static WeighedList<ItemStack> getDropTable(ItemHandler itemHandler, BPvPPlugin plugin, String config, String configKey) {
        return getDropTable(itemHandler, plugin.getConfig(config), configKey);

    }

    public static WeighedList<ItemStack> getDropTable(ItemHandler itemHandler, BPvPPlugin plugin, String configKey) {
        return getDropTable(itemHandler, plugin, "config", configKey);
    }

    public static WeighedList<ItemStack> getDropTable(ItemHandler itemHandler, ExtendedYamlConfiguration config, String configKey) {
        WeighedList<ItemStack> droptable = new WeighedList<>();

        var configSection = config.getConfigurationSection(configKey);
        if (configSection == null) return droptable;

        parseDropTable(itemHandler, configSection, droptable);

        return droptable;
    }

    public static Map<String, WeighedList<ItemStack>> getDropTables(ItemHandler itemHandler, ExtendedYamlConfiguration config, String configKey) {
        Map<String, WeighedList<ItemStack>> droptableMap = new HashMap<>();

        var configSection = config.getConfigurationSection(configKey);
        if (configSection == null) return droptableMap;

        configSection.getKeys(false).forEach(key -> {
            var droptableSection = configSection.getConfigurationSection(key);
            if(droptableSection == null) return;
            WeighedList<ItemStack> droptable = new WeighedList<>();
            parseDropTable(itemHandler, droptableSection, droptable);

            droptableMap.put(key, droptable);

        });

        return droptableMap;
    }

    private static void parseDropTable(ItemHandler itemHandler, ConfigurationSection droptableSection, WeighedList<ItemStack> droptable) {
        droptableSection.getKeys(false).forEach(key -> {
            ItemStack itemStack = null;
            int weight = droptableSection.getInt(key + ".weight");
            int categoryWeight = droptableSection.getInt(key + ".category-weight");
            int amount = droptableSection.getInt(key + ".amount", 1);

            if(key.contains(":")) {
                BPvPItem item = itemHandler.getItem(key);
                if(item != null) {
                    itemStack = item.getItemStack(amount);
                }
            }else {
                Material item = Material.getMaterial(key);
                int modelId = droptableSection.getInt(key + ".model-id", 0);
                itemStack = UtilItem.createItemStack(item, amount, modelId);
            }

            droptable.add(categoryWeight, weight, itemStack);
        });
    }

    public static void removeRecipe(Material material) {
        var iterator = Bukkit.getServer().recipeIterator();
        while(iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if(recipe.getResult().getType() == material) {
                iterator.remove();
            }
        }
    }



}
