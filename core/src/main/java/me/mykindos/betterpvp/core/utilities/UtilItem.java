package me.mykindos.betterpvp.core.utilities;

import com.google.gson.Gson;
import io.lumine.mythic.bukkit.utils.shadows.nbt.NBTTagCompound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtilItem {


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

        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);

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
        im.displayName(name);
        if (lore != null) {
            im.lore(lore);
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
     * Add the 'enchanted' glowing effect to any ItemStack
     *
     * @param item Item to update
     * @return Returns an ItemStack that is now glowing
     */
    @SuppressWarnings("deprecation")
    public static ItemStack addGlow(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        Enchantment enchantment = Enchantment.getByName("Glow");
        if (enchantment != null) {
            itemMeta.addEnchant(enchantment, 1, true);
        }

        item.setItemMeta(itemMeta);

        return item;
    }

    /**
     * Check if a Material is a type of sword
     *
     * @param swordType Material to check
     * @return Returns true if the Material is a type of sword
     */
    public static boolean isSword(Material swordType) {
        return swordType.name().contains("_SWORD");
    }

    /**
     * Check if a Material is a type of axe
     *
     * @param axeType Material to check
     * @return Returns true if the Material is a type of axe
     */
    public static boolean isAxe(Material axeType) {
        return axeType.name().contains("_AXE");
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

    public static boolean isTool(Material material) {
        return isPickaxe(material) || isHoe(material) || isShovel(material) || isAxe(material);
    }

    public static boolean isWeapon(Material material) {
        return isSword(material) || isAxe(material) || isRanged(material);
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
    public static boolean isRanged(Material wep) {
        return (wep == Material.BOW || wep == Material.CROSSBOW);
    }

    /**
     * Check if a Material is a gold tool
     *
     * @param item Material to check
     * @return Returns true if the Material is a gold tool
     */
    public static boolean isGold(Material item) {
        return (item == Material.GOLDEN_SWORD
                || item == Material.GOLDEN_AXE
                || item == Material.GOLDEN_PICKAXE
                || item == Material.GOLDEN_SHOVEL
                || item == Material.GOLDEN_HOE);
    }

    public static void insert(Player player, ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR) {
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(stack);
            } else {
                player.getWorld().dropItem(player.getLocation(), stack);
            }

            player.updateInventory();
        }
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
    public static ItemStack createItemStack(Material material, int customModelData) {
        var itemStack = new ItemStack(material);
        var itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    // Create a function to remove TextDecoration.ITALIC from List<Component>
    public static List<Component> removeItalic(List<Component> components) {
        List<Component> newComponents = new ArrayList<>();
        for (Component component : components) {
            newComponents.add(component.decoration(TextDecoration.ITALIC, false));
        }
        return newComponents;
    }

}
