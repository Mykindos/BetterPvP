package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.MDC;

import java.util.Optional;

@CustomLog
@Singleton
@SubCommand(ClanCommand.class)
public class DisbandClanSubCommand extends ClanSubCommand {

    @Inject
    public DisbandClanSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
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

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
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

            new ConfirmationMenu("Are you sure you want to disband your clan?", success -> {
                if (success) {
                    Bukkit.getPluginManager().callEvent(new ClanDisbandEvent(player, clan));
                }
            }).show(player);

        } else {
            MDC.put("clanId", clan.getId() + "");
            MDC.put("clanName", clan.getName());
            log.warn("Clan does not have a leader?").submit();
            MDC.clear();
        }


    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.LEADER;
    }
}
