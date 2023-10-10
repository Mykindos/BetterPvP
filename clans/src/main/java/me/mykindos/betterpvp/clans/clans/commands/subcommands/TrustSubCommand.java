package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestAllianceEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestTrustEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class TrustSubCommand extends ClanSubCommand {

    @Inject
    public TrustSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "trust";
    }

    @Override
    public String getDescription() {
        return "Form a trust with an ally clan, allowing them to use your clan's doors.";
    }

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
            UtilMessage.message(player, "Clans", "Only the clan admins can request a trust between clans.");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getObject(args[0]);
        if (targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "The clan you want to trust with does not exist.");
            return;
        }

        Clan targetClan = targetClanOptional.get();
        if (clan.equals(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot request a trust with your own clan.");
            return;
        }

        if (!clan.isAllied(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot request a trust with a clan you are not allied with.");
            return;
        }


        UtilServer.callEvent(new ClanRequestTrustEvent(player, clan, targetClan));

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
