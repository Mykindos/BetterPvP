package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@Singleton
@SubCommand(ClanCommand.class)
public class UnclaimSubCommand extends ClanSubCommand {

    @Inject
    public UnclaimSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getDescription() {
        return "Unclaim the territory you are standing on";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if(locationClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not standing on claimed territory");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan locationClan = locationClanOptional.get();

        if(playerClan.equals(locationClan)) {
            if(!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                UtilMessage.message(player, "Clans", "You must be an admin or above to unclaim territory");
                return;
            }

        }else{
            if(locationClan.isAdmin()) {
                UtilMessage.message(player, "Clans", "You cannot unclaim admin territory");
                return;
            }

            if(locationClan.getTerritory().size() <= clanManager.getMaximumClaimsForClan(locationClan)) {
                UtilMessage.simpleMessage(player, "Clans", "<yellow>%s<gray> has enough members to keep this territory.",
                        locationClan.getName());
                return;
            }

            for (ClanMember clanMember : locationClan.getMembers()) {
                final Optional<Client> clientOpt = clientManager.search().online(UUID.fromString(clanMember.getUuid()));
                if (clientOpt.isPresent()) {
                    final Client online = clientOpt.get();
                    if (online.isAdministrating()) {
                        UtilMessage.message(player, "Clans", "You may not unclaim territory from this Clan at this time.");
                        clientManager.sendMessageToRank("Clans",
                                UtilMessage.deserialize("<yellow>%s<gray> prevented <yellow>%s<gray> from unclaiming <yellow>%s<gray>'s territory because they are in adminstrator mode",
                                        online.getName(), player.getName(), locationClan.getName()), Rank.HELPER);
                        return;
                    }
                }
            }
        }

        UtilServer.callEvent(new ChunkUnclaimEvent(player, locationClan));
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
