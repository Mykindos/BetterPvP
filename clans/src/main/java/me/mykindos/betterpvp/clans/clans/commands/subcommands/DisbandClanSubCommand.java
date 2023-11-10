package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.MDC;

import java.util.Optional;

@Slf4j
@Singleton
@SubCommand(ClanCommand.class)
public class DisbandClanSubCommand extends ClanSubCommand {

    @Inject
    public DisbandClanSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
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

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();;
        Optional<ClanMember> leaderOptional = clan.getLeader();
        if (leaderOptional.isPresent()) {
            ClanMember leader = leaderOptional.get();

            if (!leader.getUuid().equalsIgnoreCase(client.getUuid()) && !client.isAdministrating()) {
                UtilMessage.message(player, "Command", "You do not have permission to disband this clan");
                return;
            }

            if(clanManager.getPillageHandler().isBeingPillaged(clan)){
                UtilMessage.message(player, "Clans", "You cannot disband your clan while being pillaged.");
                return;
            }

            if (System.currentTimeMillis() < clan.getLastTntedTime()) {
                UtilMessage.simpleMessage(player, "Clans", "You cannot disband your clan for <alt>" + UtilTime.getTime(clan.getLastTntedTime() - System.currentTimeMillis(), UtilTime.TimeUnit.BEST, 1));
                return;
            }

            new ConfirmationMenu("Are you sure you want to disband your clan?", success -> {
                if (success) {
                    Bukkit.getPluginManager().callEvent(new ClanDisbandEvent(player, clan));
                }
            }).show(player);

        } else {
            MDC.put("clanId", clan.getId() + "");
            MDC.put("clanName", clan.getName());
            log.warn("Clan does not have a leader?");
            MDC.clear();
        }


    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.LEADER;
    }
}
