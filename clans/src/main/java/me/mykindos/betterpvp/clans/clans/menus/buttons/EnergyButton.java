package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
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

public class EnergyButton extends Button {

    private final Clan clan;
    private boolean ownClan;

    public EnergyButton(int slot, Player player, Clan clan) {
        super(slot, new ItemStack(Material.PAPER));
        this.clan = clan;
        this.ownClan = clan.getMemberByUUID(player.getUniqueId()).isPresent();

        this.name = Component.text("Energy",NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false);
        this.lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("%d / ∞", clan.getEnergy()));
        if (ownClan) {
            lore.add(Component.text(""));
            lore.add(Component.text("Left click to open the energy shop", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        }

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();

    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (!ownClan) return;

        if (clickType.isLeftClick()) {
            // TODO add clan shop menu later
        }
    }


}
