package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;

public class AddOutskirtsSubCommand extends ClanSubCommand {

    public AddOutskirtsSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "addoutskirts";
    }

    @Override
    public String getDescription() {
        return "Adds outskirt claims around all of your claims";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!client.hasRank(Rank.ADMIN)) {
            UtilMessage.message(player, "Clans", "Uh oh!");
            return;
        }

        int borderSize = 1;
        if (args.length > 0) {
            borderSize = Integer.parseInt(args[0]);
        }
        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "You are not in a clan");
            return;
        }

        Optional<Clan> outskirtsOptional = clanManager.getClanByName("Outskirts");
        if (outskirtsOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Outskirts clan does not exist, create it first.");
            return;
        }

        Clan clan = clanOptional.get();
        Clan outskirts = outskirtsOptional.get();

        int claims = 0;
        for (ClanTerritory territory : clan.getTerritory()) {
            Chunk chunk = UtilWorld.stringToChunk(territory.getChunk());
            if (chunk == null) continue;
            for (int x = -borderSize; x <= borderSize; x++) {
                for (int z = -borderSize; z <= borderSize; z++) {
                    if (x == 0 && z == 0) continue;
                    Chunk wildernessChunk = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
                    Optional<Clan> locationClanOptional = clanManager.getClanByChunk(wildernessChunk);
                    if (locationClanOptional.isEmpty()) {

                        UtilServer.callEvent(new ChunkClaimEvent(player, outskirts, wildernessChunk));
                        claims++;
                    }
                }
            }
        }

        UtilMessage.simpleMessage(player, "Clans", "Added <yellow>%s<gray> claims to the outskirts", claims);
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
