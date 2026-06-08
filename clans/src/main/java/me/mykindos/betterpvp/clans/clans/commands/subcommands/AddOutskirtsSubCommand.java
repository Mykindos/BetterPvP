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
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AddOutskirtsSubCommand extends ClanSubCommand {

    @Inject
    public AddOutskirtsSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "addoutskirts";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " [borderSize]";
    }

    @Override
    public String getDescription() {
        return "clans.command.add-outskirts.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        int borderSize = 1;
        if (args.length > 0) {
            try {
                borderSize = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.add-outskirts.not-integer", Component.text(args[0], NamedTextColor.YELLOW));
                return;
            }
        }

        Optional<Clan> outskirtsOptional = clanManager.getClanByName("Outskirts");
        if (outskirtsOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.add-outskirts.no-outskirts-clan");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();
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
        final int finalClaims = claims;
        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.add-outskirts.success", Component.text(finalClaims, NamedTextColor.YELLOW));
        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
            UtilMessage.message(target, CLANS_PREFIX, "clans.command.clan.add-outskirts.notification",
                    Component.text(player.getName(), NamedTextColor.YELLOW), Component.text(finalClaims, NamedTextColor.YELLOW));
        });
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }

}
