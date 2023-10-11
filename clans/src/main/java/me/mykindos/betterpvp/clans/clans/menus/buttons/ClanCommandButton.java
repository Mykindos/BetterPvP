package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.menu.Button;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class ClanCommandButton extends Button {

    public ClanCommandButton(int slot) {
        super(slot, new ItemStack(Material.PAPER));

        this.name = Component.text("Clan Commands", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC,false);
        this.lore = new ArrayList<>();
        lore.add(Component.text("/c claim (claims territory)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c sethome (sets your home)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c ally (allies a clan)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c enemy (enemies a clan)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c demote (demotes a member)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c promote (promotes a member)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c invite (invites a player)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("/c home (teleport to clan home)",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        lore.add(Component.text("Left click to show more",NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));






        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            player.chat("/clan help");
        }
    }


}
