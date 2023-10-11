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

public class LeaveClanButton extends Button {

    private final ClanMember.MemberRank rank; // to store the rank of the player in the clan

    public LeaveClanButton(int slot, Clan clan, Player player) {
        super(slot, new ItemStack(Material.PAPER));

        ClanMember member = clan.getMemberByUUID(player.getUniqueId()).orElse(null);
        this.rank = member != null ? member.getRank() : null;

        this.name = Component.text("Leave Clan", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
        this.lore = new ArrayList<>();
        lore.add(Component.text("Left click to leave your clan", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        if (rank == ClanMember.MemberRank.LEADER) {
            lore.add(Component.text("Shift Left click to disband your clan", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }

        this.itemStack = UtilItem.removeAttributes(UtilItem.setItemNameAndLore(itemStack, name, lore)).clone();
    }

    @Override
    public void onClick(Player player, Gamer gamer, ClickType clickType) {
        if (clickType.isLeftClick()) {
            player.chat("/clan leave");
        } else if (clickType.isShiftClick() && rank == ClanMember.MemberRank.LEADER) {
            player.chat("/c disband");
        }
    }
}

