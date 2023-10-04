package me.mykindos.betterpvp.clans.clans.menus.buttons;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
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

public class TerritoryButton extends Button {

    private final boolean ownClan;
    private final ClanMember.MemberRank rank; // to store the rank of the player in the clan

    public TerritoryButton(int slot, Player player, Clan clan) {
        super(slot, new ItemStack(Material.PAPER));
        ClanMember member = clan.getMemberByUUID(player.getUniqueId()).orElse(null);
        this.ownClan = member != null;
        this.rank = member != null ? member.getRank() : null;

        this.name = Component.text("Territory", NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC,false);
        this.lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("%d/%d claimed", clan.getTerritory().size(), Math.min(clan.getMembers().size() + 3, 9)));

        if (ownClan && (rank == ClanMember.MemberRank.ADMIN || rank == ClanMember.MemberRank.LEADER)) {
            lore.add(Component.text(""));
            lore.add(Component.text("Left click to claim territory", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
            lore.add(Component.text(""));
            lore.add(Component.text("Right click to unclaim territory", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,false));
        }

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (!ownClan) return;
        if (rank != ClanMember.MemberRank.ADMIN && rank != ClanMember.MemberRank.LEADER) return;

        if (clickType.isLeftClick()) {
            player.chat("/clan claim");
        } else if (clickType.isRightClick()) {
            player.chat("/clan unclaim");
        }
    }
}
