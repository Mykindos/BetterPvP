package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.utilities.UtilClans;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.List;
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
            if(locationClan.getTerritory().size() > 2 && !locationClan.isAdmin()){
                List<ClanTerritory> territoryChunks = locationClan.getTerritory();

                // Logic to get the width and length of the 2d array.
                int maxX = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;

                for (ClanTerritory territoryChunk : territoryChunks) {
                    Chunk c = UtilWorld.stringToChunk(territoryChunk.getChunk());
                    maxX = Math.max(maxX, c.getX());
                    maxZ = Math.max(maxZ, c.getZ());
                    minX = Math.min(minX, c.getX());
                    minZ = Math.min(minZ, c.getZ());
                }

                // Logic to map out the clans territory in a 2d array.
                int[][] territoryGrid = new int[Math.abs(maxX - minX + 1)][Math.abs(maxZ - minZ + 1)];

                for (ClanTerritory territoryChunk : territoryChunks) {
                    Chunk c = UtilWorld.stringToChunk(territoryChunk.getChunk());
                    if (!(c.getX() == player.getChunk().getX() && c.getZ() == player.getChunk().getZ())) {
                        territoryGrid[c.getX() - minX][c.getZ() - minZ] = 1;
                    }
                }

                // Pass territory 2d array into algorithm.
                if(UtilClans.isClaimLegal(territoryGrid)){
                    UtilMessage.message(player, "Clans", "You cannot unclaim a chunk that splits up other chunks.");
                    return;
                }
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
