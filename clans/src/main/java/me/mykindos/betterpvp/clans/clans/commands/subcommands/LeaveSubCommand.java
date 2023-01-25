package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LeaveSubCommand extends ClanSubCommand {

    public LeaveSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Leave the clan you are currently in";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (clan.isEnemy(locationClan)) {
                UtilMessage.message(player, "Clans", "You cannot leave your clan while in enemy territory");
                return;
            }
        }

        if (!clan.isAdmin()) {
            Optional<ClanMember> leaderOptional = clan.getLeader();
            if (leaderOptional.isPresent()) {
                ClanMember leader = leaderOptional.get();
                if (leader.getUuid().equals(player.getUniqueId().toString())) {
                    if (clan.getMembers().size() > 1) {
                        UtilMessage.message(player, "Clans", "You must pass on " + ChatColor.GREEN + "Leadership" + ChatColor.GRAY + " before leaving.");
                        return;
                    } else if (clan.getMembers().size() == 1) {
                        UtilServer.callEvent(new ClanDisbandEvent(player, clan));
                        return;
                    }
                }

            }
        }

        if (System.currentTimeMillis() < clan.getLastTnted()) {
            UtilMessage.message(player, "Clans", "You cannot leave your clan for "
                    + ChatColor.GREEN + UtilTime.getTime(clan.getLastTnted() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1));
            return;
        }

        UtilServer.callEvent(new MemberLeaveClanEvent(player, clan));

    }
}
