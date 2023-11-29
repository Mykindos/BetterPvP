package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestNeutralEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class NeutralSubCommand extends ClanSubCommand {

    @Inject
    public NeutralSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "neutral";
    }

    @Override
    public String getDescription() {
        return "Request a neutral status with another clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan to request neutral status with.");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getObject(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "The target clan does not exist.");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan targetClan = targetClanOptional.get();

        if(playerClan.equals(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot neutral your own clan");
            return;
        }

        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "Only the clan admins can form alliances.");
            return;
        }

        if(!playerClan.isEnemy(targetClan) && !playerClan.isAllied(targetClan)) {
            UtilMessage.message(player, "Clans", "You are already neutral with this clan.");
            return;
        }

        UtilServer.callEvent(new ClanRequestNeutralEvent(player, playerClan, targetClan));
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
