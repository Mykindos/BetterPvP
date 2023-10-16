package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class DemoteSubCommand extends ClanSubCommand {

    @Inject
    public DemoteSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "demote";
    }

    @Override
    public String getDescription() {
        return "Demote a member of your clan to a lower rank";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a player to demote.");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();;

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRank().getPrivilege() < ClanMember.MemberRank.ADMIN.getPrivilege()) {
            UtilMessage.message(player, "Clans", "You do not have permission to do this.");
            return;
        }

        String targetMemberName = args[0];
        if(player.getName().equalsIgnoreCase(targetMemberName)) {
            UtilMessage.message(player, "Clans", "You cannot demote yourself.");
            return;
        }

        Optional<Gamer> targetGamerOptional = gamerManager.getGamerByName(targetMemberName);
        if (targetGamerOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a player with that name");
            return;
        }

        Gamer targetGamer = targetGamerOptional.get();
        clan.getMemberByUUID(targetGamer.getUuid()).ifPresentOrElse(targetMember -> {
            if (targetMember.getRank().getPrivilege() >= member.getRank().getPrivilege()
                    && !client.isAdministrating()) {
                UtilMessage.message(player, "Clans", "You can only demote players with a lower rank.");
                return;
            }
            else if (client.isAdministrating() && targetMember.getRank().getPrivilege() >= member.getRank().getPrivilege()) {
                gamerManager.sendMessageToRank("Clans",
                        UtilMessage.deserialize("<yellow>%s<gray> force demoted <yellow>%s", player.getName(), targetGamer.getPlayer().getName()),
                        Rank.HELPER);
            }

            UtilServer.callEvent(new MemberDemoteEvent(player, clan, targetMember));
        }, () -> {
            UtilMessage.message(player, "Clans", "That player is not in your clan.");
        });

    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ClanArgumentType.CLAN_MEMBER.name() : ArgumentType.NONE.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
