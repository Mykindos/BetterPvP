package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierUnclaimCommand extends ClanBrigadierCommand {
    @Inject
    protected BrigadierUnclaimCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "unclaim";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Unclaims the specified territory";
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
                .executes(context -> {
                    final Player executor = getPlayerFromExecutor(context);
                    final Clan executorClan = getClanByExecutor(context);
                    final Optional<Clan> locationClanOptional = clanManager.getClanByLocation(executor.getLocation());
                    if (locationClanOptional.isEmpty()) {
                        throw ClanArgumentException.NOT_ON_CLAIMED_TERRITORY.create();
                    }
                    final Clan locationClan = locationClanOptional.get();

                    //administrating clients can always unclaim territory
                    if (senderIsAdministrating(context.getSource())) {
                        UtilServer.callEvent(new ChunkUnclaimEvent(executor, locationClan));
                        return Command.SINGLE_SUCCESS;
                    }


                    //non-standard unclaim
                    if (!executorClan.equals(locationClan)) {
                        clanManager.canUnclaimOtherThrow(executor, locationClan);

                        UtilServer.callEvent(new ChunkUnclaimEvent(executor, locationClan));

                        return Command.SINGLE_SUCCESS;
                    }

                    //must have admin or leader
                    if (!executorClan.getMember(executor.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                        throw ArgumentException.INSUFFICIENT_PERMISSION.create();
                    }

                    clanManager.canUnclaimOwnThrow(executor, executorClan);
                    UtilServer.callEvent(new ChunkUnclaimEvent(executor, locationClan));

                    return Command.SINGLE_SUCCESS;
                });
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && (executorHasAClan(source) || senderIsAdministrating(source));
    }
}
