package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AddOutskirtsSubCommand extends ClanSubCommand {

    @Inject
    public AddOutskirtsSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "addoutskirts";
    }

    public String getUsage() {
        return super.getUsage() + " [borderSize]";
    }

    @Override
    public String getDescription() {
        return "Adds outskirt claims around all of your claims";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        int borderSize = 1;
        if (args.length > 0) {
            try {
                borderSize = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                UtilMessage.message(player, "Clans", "<yellow>%s<gray> is not an integer", args[0]);
                return;
            }
        }

        Optional<Clan> outskirtsOptional = clanManager.getClanByName("Outskirts");
        if (outskirtsOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Outskirts clan does not exist, create it first");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();;
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
        String message = "Added <yellow>%s<gray> claims to the outskirts";

        UtilMessage.simpleMessage(player, "Clans", message, claims);
        gamerManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> " + message.toLowerCase(), player.getName(), claims), Rank.HELPER);
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
