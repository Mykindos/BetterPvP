package me.mykindos.betterpvp.clans.clans.tips.tips;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.tips.Tip;
import me.mykindos.betterpvp.clans.clans.tips.types.ISuggestCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ClanInviteTip extends Tip implements ISuggestCommand {

    ClanInviteTip() {
        super(2, 1);
        setComponent(generateComponent());
    }

    private Component generateComponent() {
        Component suggestComponent = suggestCommand("/c invite <player>", "/c invite ");
        Component component = Component.text("You can invite a player to your clan by running ", NamedTextColor.GRAY).append(suggestComponent);
        return component;
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN);
    }
}
