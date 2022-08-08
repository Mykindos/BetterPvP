package me.mykindos.betterpvp.clans.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.weapons.WeaponManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ItemHandler {

    private WeaponManager weaponManager;

    @Inject
    public ItemHandler(WeaponManager weaponManager) {
        this.weaponManager = weaponManager;
    }

    /**
     * General method that updates the name of almost every item that is picked up by players
     * E.g. Names leather armour after assassins
     * E.g. Turns the colour of the items name to yellow from white
     *
     * @param item ItemStack to update
     * @return An ItemStack with an updated name
     */
    public ItemStack updateNames(ItemStack item) {

        // TODO weapon impl
        //if (WeaponManager.getWeapon(item) != null) {
        //    return item;
        //}

        if (item.hasItemMeta()) {
            //if(Perk.getPerk(item.getItemMeta().getDisplayName()) != null){
            //    return item;
            // }
        }
        List<String> lore = new ArrayList<>();
        Material m = item.getType();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);


            // TODO load from database
            if (m == Material.LEATHER_HELMET) {
                meta.setDisplayName("Assassin Helmet");
            } else if (m == Material.LEATHER_CHESTPLATE) {
                meta.setDisplayName("Assassin Vest");
            } else if (m == Material.LEATHER_LEGGINGS) {
                meta.setDisplayName("Assassin Leggings");
            } else if (m == Material.LEATHER_BOOTS) {
                meta.setDisplayName("Assassin Boots");
            } else if (m == Material.IRON_HELMET) {
                meta.setDisplayName("Knight Helmet");
            } else if (m == Material.IRON_CHESTPLATE) {
                meta.setDisplayName("Knight Chestplate");
            } else if (m == Material.IRON_LEGGINGS) {
                meta.setDisplayName("Knight Leggings");
            } else if (m == Material.IRON_BOOTS) {
                meta.setDisplayName("Knight Boots");
            } else if (m == Material.DIAMOND_HELMET) {
                meta.setDisplayName("Gladiator Helmet");
            } else if (m == Material.DIAMOND_CHESTPLATE) {
                meta.setDisplayName("Gladiator Chestplate");
            } else if (m == Material.DIAMOND_LEGGINGS) {
                meta.setDisplayName("Gladiator Leggings");
            } else if (m == Material.DIAMOND_BOOTS) {
                meta.setDisplayName("Gladiator Boots");
            } else if (m == Material.GOLDEN_HELMET) {
                meta.setDisplayName("Paladin Helmet");
            } else if (m == Material.GOLDEN_CHESTPLATE) {
                meta.setDisplayName("Paladin Vest");
            } else if (m == Material.GOLDEN_LEGGINGS) {
                meta.setDisplayName("Paladin Leggings");
            } else if (m == Material.GOLDEN_BOOTS) {
                meta.setDisplayName("Paladin Boots");
            } else if (m == Material.NETHERITE_HELMET) {
                meta.setDisplayName("Warlock Helmet");
            } else if (m == Material.NETHERITE_CHESTPLATE) {
                meta.setDisplayName("Warlock Vest");
            } else if (m == Material.NETHERITE_LEGGINGS) {
                meta.setDisplayName("Warlock Leggings");
            } else if (m == Material.NETHERITE_BOOTS) {
                meta.setDisplayName("Warlock Boots");
            } else if (m == Material.CHAINMAIL_HELMET) {
                meta.setDisplayName("Ranger Helmet");
            } else if (m == Material.CHAINMAIL_CHESTPLATE) {
                meta.setDisplayName("Ranger Vest");
            } else if (m == Material.CHAINMAIL_LEGGINGS) {
                meta.setDisplayName("Ranger Leggings");
            } else if (m == Material.CHAINMAIL_BOOTS) {
                meta.setDisplayName("Ranger Boots");
            } else if (m == Material.GOLDEN_AXE) {
                meta.setDisplayName("Radiant axe");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "6");

            } else if (m == Material.MUSIC_DISC_WAIT) {
                meta.setDisplayName("$100,000");
            } else if (m == Material.MUSIC_DISC_13) {
                meta.setDisplayName("$50,000");
            } else if (m == Material.MUSIC_DISC_PIGSTEP) {
                meta.setDisplayName("$1,000,000");
            } else if (m == Material.CARROT) {
                meta.setDisplayName("Carrot");
            } else if (m == Material.POTATO) {
                meta.setDisplayName("Potato");
            } else if (m == Material.IRON_SWORD) {

                meta.setDisplayName("Standard Sword");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "5");

            } else if (m == Material.GOLDEN_SWORD) {
                meta.setDisplayName("Radiant Sword");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "7");
            } else if (m == Material.DIAMOND_SWORD) {
                meta.setDisplayName("Power Sword");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "6");
                lore.add(ChatColor.GRAY + "Bonus 1 Level to Sword Skills");
            } else if (m == Material.NETHERITE_SWORD) {
                meta.setDisplayName("Ancient Sword");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "7");
                lore.add(ChatColor.GRAY + "Bonus 1 Level to Sword Skills");
            } else if (m == Material.IRON_AXE) {
                meta.setDisplayName("Standard Axe");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "3");

            } else if (m == Material.DIAMOND_AXE) {
                meta.setDisplayName("Power Axe");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "5");
                lore.add(ChatColor.GRAY + "Bonus 1 Level to Axe Skills");
            } else if (m == Material.NETHERITE_AXE) {
                meta.setDisplayName("Ancient Axe");
                lore.add(ChatColor.GRAY + "Damage: " + ChatColor.GREEN + "6");
                lore.add(ChatColor.GRAY + "Bonus 1 Level to Axe Skills");
            } else if (m == Material.TURTLE_HELMET) {
                meta.setDisplayName("Agility Helmet");
            } else if (m == Material.FILLED_MAP) {
                meta.displayName(Component.text("Map"));
            }
            if (meta.hasDisplayName()) {
                if (meta.getDisplayName().equalsIgnoreCase(ChatColor.stripColor("Base Fishing"))) {
                    lore.add(ChatColor.WHITE + "Allows meta player to fish inside their base");
                }
                if (!meta.getDisplayName().contains("Crate")) {
                    meta.setDisplayName(ChatColor.YELLOW + ChatColor.stripColor(meta.getDisplayName()));
                }
            } else {
                meta.setDisplayName(ChatColor.YELLOW + UtilFormat.cleanString(item.getType().name()));
            }
        }
        meta.setLore(lore);


        item.setItemMeta(meta);
        return item;
    }
}
