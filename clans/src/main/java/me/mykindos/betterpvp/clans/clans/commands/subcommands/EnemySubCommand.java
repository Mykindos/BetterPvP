package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class EnemySubCommand extends ClanSubCommand {

    @Inject
    public EnemySubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "enemy";
    }

    @Override
    public String getDescription() {
        return "Wage war with another clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a clan to enemy.");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getObject(args[0]);
        if (targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "The target clan does not exist.");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();;
        Clan targetClan = targetClanOptional.get();

        if (playerClan.equals(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot enemy your own clan");
            return;
        }

        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "Only the clan admins can enemy other clans.");
            return;
        }

        if (playerClan.isEnemy(targetClan)) {
            UtilMessage.message(player, "Clans", "You are already enemy with this clan.");
            return;
        }

        if(playerClan.isAllied(targetClan)) {
            UtilMessage.message(player, "Clans", "You cannot enemy a clan you are allied with.");
            return;
        }

        if (clanManager.getPillageHandler().isPillaging(playerClan, targetClan)
                || clanManager.getPillageHandler().isPillaging(targetClan, playerClan)) {
            UtilMessage.message(player, "Clans", "You cannot enemy this clan while a pillage is active.");
            return;
        }

        UtilServer.callEvent(new ClanEnemyEvent(player, playerClan, targetClan));
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
