package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
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
public class EnemySubCommand extends ClanSubCommand {

    @Inject
    public EnemySubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "enemy";
    }

    @Override
    public String getDescription() {
        return "clans.command.enemy.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.no-args");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if (targetClanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.not-found");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan targetClan = targetClanOptional.get();

        if (playerClan.equals(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.self");
            return;
        }

        if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.no-rank");
            return;
        }

        if (playerClan.isEnemy(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.already-enemy");
            return;
        }

        if(playerClan.isAllied(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.allied");
            return;
        }

        if (clanManager.getPillageHandler().isPillaging(playerClan, targetClan)
                || clanManager.getPillageHandler().isPillaging(targetClan, playerClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.enemy.pillage");
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
