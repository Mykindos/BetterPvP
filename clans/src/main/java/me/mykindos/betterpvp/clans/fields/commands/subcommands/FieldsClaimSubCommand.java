package me.mykindos.betterpvp.clans.fields.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.fields.commands.FieldsCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(FieldsCommand.class)
public class FieldsClaimSubCommand extends Command {

    @Inject
    private ClanManager clanManager;

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claim land for fields";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        int borderSize = 1;
        if (args.length > 0) {
            borderSize = Integer.parseInt(args[0]);
        }

        Optional<Clan> fieldsOptional = clanManager.getClanByName("Fields");
        if (fieldsOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Fields clan does not exist, create it first.");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan outskirts = fieldsOptional.get();

        int claims = 0;
        for (ClanTerritory territory : clan.getTerritory()) {
            Chunk chunk = territory.getWorldChunk();
            if (chunk == null) {
                continue;
            }

            for (int x = -borderSize; x <= borderSize; x++) {
                for (int z = -borderSize; z <= borderSize; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }

                    Chunk wildernessChunk = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
                    Optional<Clan> locationClanOptional = clanManager.getClanByChunk(wildernessChunk);
                    if (locationClanOptional.isEmpty()) {
                        UtilServer.callEvent(new ChunkClaimEvent(player, outskirts, wildernessChunk));
                        claims++;
                    }
                }
            }
        }

        UtilMessage.simpleMessage(player, "Clans", "Added <yellow>%s<gray> claims to the Fields clan.", claims);
    }

}
