package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.slf4j.MDC;

import java.util.Optional;

@Slf4j
public class DisbandClanSubCommand extends ClanSubCommand {


    public DisbandClanSubCommand(ClanManager clanManager) {
        super(clanManager);
    }

    @Override
    public String getName() {
        return "disband";
    }

    @Override
    public String getDescription() {
        return "Disbands your current clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Optional<Clan> clanOptional = clanManager.getClanByClient(client);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Command", "You are not in a clan.");
            return;
        }

        Clan clan = clanOptional.get();
        Optional<ClanMember> leaderOptional = clan.getLeader();
        if (leaderOptional.isPresent()) {
            ClanMember leader = leaderOptional.get();

            if (!leader.getUuid().equalsIgnoreCase(client.getUuid()) && !client.isAdministrating()) {
                UtilMessage.message(player, "Command", "You do not have permission to disband this clan");
                return;
            }

            // TODO implement pillaging

            if (System.currentTimeMillis() < clan.getLastTnted()) {
                UtilMessage.message(player, "Clans", "You cannot disband your clan for "
                        + ChatColor.GREEN + UtilTime.getTime(clan.getLastTnted() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1));
                return;
            }

            Bukkit.getPluginManager().callEvent(new ClanDisbandEvent(player, clan));

        } else {
            MDC.put("clanId", clan.getId() + "");
            MDC.put("clanName", clan.getName());
            log.warn("Clan does not have a leader?");
            MDC.clear();
        }


    }
}
