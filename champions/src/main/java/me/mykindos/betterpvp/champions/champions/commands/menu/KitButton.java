package me.mykindos.betterpvp.champions.champions.commands.menu;

import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class KitButton extends Button {

    private final Role role;
    public KitButton(int slot, ItemStack item, String name, Role role) {
        super(slot, item, Component.text(name, NamedTextColor.GREEN));

        this.role = role;
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(role.getHelmet()));
        player.getInventory().setChestplate(new ItemStack(role.getChestplate()));
        player.getInventory().setLeggings(new ItemStack(role.getLeggings()));
        player.getInventory().setBoots(new ItemStack(role.getBoots()));

        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
        player.getInventory().addItem(new ItemStack(Material.BOW));
        player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
        player.getInventory().addItem(new ItemStack(Material.BOOK));

        player.closeInventory();
    }
}
