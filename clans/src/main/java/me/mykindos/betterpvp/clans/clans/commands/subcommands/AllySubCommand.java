package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestAllianceEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AllySubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    public AllySubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "ally";
    }

    @Override
    public String getDescription() {
        return "Form an alliance with another clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You did not input a clan name");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "Only the clan admins can form alliances.");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if (targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "The clan you want to ally with does not exist.");
            return;
        }

        Clan targetClan = targetClanOptional.get();
        if (clan.equals(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot ally with your own clan.");
            return;
        }

        if (clan.isAllied(targetClan)) {
            UtilMessage.message(player, "Clans", "You are already allied with this clan.");
            return;
        }

        if(clan.isEnemy(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot ally with a clan you are at war with.");
            return;
        }

        if (clan.getSquadCount() >= maxClanMembers) {
            UtilMessage.message(player, "Clans", "Your clan is at the maximum squad size (<green>%s</green>), you cannot ally another clan.", clan.getSquadCount());
            return;
        }

        int ownClanSize = clan.getMembers().size();
        if (ownClanSize + targetClan.getSquadCount() > maxClanMembers) {
            UtilMessage.simpleMessage(player, "Clans",
                    "<yellow>%s<gray> has too many members and allies (<green>%</green>) to ally your clan (<green>%s</green>)",
                    targetClan.getName(), targetClan.getSquadCount(), ownClanSize);
            return;
        }

        int targetClanSize = targetClan.getMembers().size();
        if (targetClanSize + clan.getSquadCount() > maxClanMembers) {
            UtilMessage.message(player, "Clans",
                    "Your clan has too many members and allies (<green>%s</green>) to ally <yellow>%s</yellow> (<green>%s</green>)",
                    clan.getSquadCount(), targetClan.getName(), targetClanSize);
            return;
        }

        UtilServer.callEvent(new ClanRequestAllianceEvent(player, clan, targetClan));

    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

}
