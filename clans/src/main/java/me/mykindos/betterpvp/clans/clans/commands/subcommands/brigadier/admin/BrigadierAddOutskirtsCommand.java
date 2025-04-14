package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

@CustomLog
@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierAddOutskirtsCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierAddOutskirtsCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "addoutskirts";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Adds outskirt claims around all of your claims";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public BPvPLiteralArgumentBuilder define() {
        return IBrigadierCommand.literal(getName())
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan())
                        .then(IBrigadierCommand.argument("radius", IntegerArgumentType.integer(1))
                            .executes(context -> {
                                final Clan target = context.getArgument("Clan", Clan.class);
                                final int radius = context.getArgument("radius", Integer.class);
                                final Player player = getPlayerFromExecutor(context);

                                Optional<Clan> outskirtsOptional = clanManager.getClanByName("Outskirts");
                                if (outskirtsOptional.isEmpty()) {
                                    throw ClanArgumentException.CLAN_DOES_NOT_EXIST.create("Outskirts");
                                }

                                Clan outskirts = outskirtsOptional.get();

                                int claims = 0;
                                for (ClanTerritory territory : target.getTerritory()) {
                                    Chunk chunk = UtilWorld.stringToChunk(territory.getChunk());
                                    if (chunk == null) continue;
                                    for (int x = -radius; x <= radius; x++) {
                                        for (int z = -radius; z <= radius; z++) {
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
                                clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> " + message.toLowerCase(), player.getName(), claims), Rank.HELPER);
                                return Command.SINGLE_SUCCESS;
                            })
                        )
                );
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        boolean bool = super.requirement(source);
        log.info("add outskirts requirement {}", bool).submit();
        return bool;
    }
}
