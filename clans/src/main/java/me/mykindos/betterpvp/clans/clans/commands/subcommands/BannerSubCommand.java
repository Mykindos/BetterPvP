package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.menus.BannerMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.List;

@Singleton
@SubCommand(ClanCommand.class)
public class BannerSubCommand extends ClanSubCommand {

    @Inject
    public BannerSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
        aliases.addAll(List.of("banner", "setclanbanner"));
    }

    @Override
    public String getName() {
        return "setbanner";
    }

    @Override
    public String getDescription() {
        return "Set your clans banner";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        // Check if player is in a clan and is a leader or admin
        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            UtilMessage.message(player, "Banner", "You are not in a clan!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRank() != ClanMember.MemberRank.ADMIN && member.getRank() != ClanMember.MemberRank.LEADER) {
            UtilMessage.message(player, "Banner", "Only admins and leaders can set the clan banner!");
            return;
        }

        new BannerMenu(clan, null).show(player);
    }

    @Override
    public String getArgumentType(int arg) {
        return ArgumentType.NONE.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
