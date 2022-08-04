package me.mykindos.betterpvp.clans.champions.builds.menus;

import me.mykindos.betterpvp.clans.champions.builds.menus.buttons.ClassSelectionButton;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Menu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ClassSelectionMenu extends Menu {
    public ClassSelectionMenu(Player player, Gamer gamer) {
        super(player, 36, "Class Customisation",
                new ClassSelectionButton[]{
                        new ClassSelectionButton(gamer, 0, new ItemStack(Material.DIAMOND_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Gladiator Class"),
                        new ClassSelectionButton(gamer, 9, new ItemStack(Material.DIAMOND_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Gladiator Class"),
                        new ClassSelectionButton(gamer, 18, new ItemStack(Material.DIAMOND_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Gladiator Class"),
                        new ClassSelectionButton(gamer, 27, new ItemStack(Material.DIAMOND_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Gladiator Class"),

                        new ClassSelectionButton(gamer, 1, new ItemStack(Material.LEATHER_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Assassin Class"),
                        new ClassSelectionButton(gamer, 10, new ItemStack(Material.LEATHER_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Assassin Class"),
                        new ClassSelectionButton(gamer, 19, new ItemStack(Material.LEATHER_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Assassin Class"),
                        new ClassSelectionButton(gamer, 28, new ItemStack(Material.LEATHER_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Assassin Class"),

                        new ClassSelectionButton(gamer, 3, new ItemStack(Material.IRON_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Knight Class"),
                        new ClassSelectionButton(gamer, 12, new ItemStack(Material.IRON_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Knight Class"),
                        new ClassSelectionButton(gamer, 21, new ItemStack(Material.IRON_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Knight Class"),
                        new ClassSelectionButton(gamer, 30, new ItemStack(Material.IRON_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Knight Class"),

                        new ClassSelectionButton(gamer, 5, new ItemStack(Material.NETHERITE_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Warlock Class"),
                        new ClassSelectionButton(gamer, 14, new ItemStack(Material.NETHERITE_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Warlock Class"),
                        new ClassSelectionButton(gamer, 23, new ItemStack(Material.NETHERITE_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Warlock Class"),
                        new ClassSelectionButton(gamer, 32, new ItemStack(Material.NETHERITE_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Warlock Class"),

                        new ClassSelectionButton(gamer, 7, new ItemStack(Material.CHAINMAIL_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Ranger Class"),
                        new ClassSelectionButton(gamer, 16, new ItemStack(Material.CHAINMAIL_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Ranger Class"),
                        new ClassSelectionButton(gamer, 25, new ItemStack(Material.CHAINMAIL_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Ranger Class"),
                        new ClassSelectionButton(gamer, 34, new ItemStack(Material.CHAINMAIL_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Ranger Class"),
                        new ClassSelectionButton(gamer, 8, new ItemStack(Material.GOLDEN_HELMET).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Paladin Class"),
                        new ClassSelectionButton(gamer, 17, new ItemStack(Material.GOLDEN_CHESTPLATE).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Paladin Class"),
                        new ClassSelectionButton(gamer, 26, new ItemStack(Material.GOLDEN_LEGGINGS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Paladin Class"),
                        new ClassSelectionButton(gamer, 35, new ItemStack(Material.GOLDEN_BOOTS).clone(), ChatColor.GREEN.toString() + ChatColor.BOLD + "Paladin Class")});
    }
}
