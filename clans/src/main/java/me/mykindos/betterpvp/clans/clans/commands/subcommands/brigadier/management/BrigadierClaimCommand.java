package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierClaimCommand extends BrigadierClanSubCommand {
    @Inject
    protected BrigadierClaimCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
        this.getAliases().add("c");
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "claim";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Claims the specified territory";
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return Commands.literal(getName())
                .executes(context -> {
                    final Player executor = getPlayerFromExecutor(context);
                    final Clan executorClan = getClanByExecutor(context);
                    final Chunk chunk = executor.getChunk();

                    if (senderIsAdministrating(context.getSource())) {
                        final Optional<Clan> locationClanOptional = clanManager.getClanByChunk(chunk);
                        if (locationClanOptional.isPresent()) {
                            final Clan locationClan = locationClanOptional.get();
                            throw ClanArgumentException.CLAN_ALREADY_CLAIMS_TERRITORY.create(locationClan);
                        }
                        clientManager.sendMessageToRank("Clans",
                                UtilMessage.deserialize("<yellow>%s</yellow> force claimed territory for <yellow>%s</yellow>",
                                        context.getSource().getSender().getName(), executorClan.getName()),
                                Rank.HELPER);
                    } else {
                        clanManager.canClaimThrow(executor, executorClan, chunk);
                    }

                    UtilServer.callEvent(new ChunkClaimEvent(executor, executorClan));

                    return Command.SINGLE_SUCCESS;
                });
    }


}
