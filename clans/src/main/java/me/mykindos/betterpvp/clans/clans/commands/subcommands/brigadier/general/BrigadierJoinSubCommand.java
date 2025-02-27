package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierJoinSubCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierJoinSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "join";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Joins the specified clan";
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
                .then(Commands.argument("Joinable Clan", BPvPClansArgumentTypes.invitablePlayer())
                        .executes(context -> {
                            final Clan target = context.getArgument("Joinable Clan", Clan.class);

                            if (!(context.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;

                            final Client client = getClientFromExecutor(context);

                            clanManager.canJoinClan(client, target);

                            doJoin(player, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        //TODO admin join as separate argument
                        .requires(this::executorHasAClan)
                );
    }

    /**
     *
     * @param joiner the player joining
     * @param clan the clan that this join is happening in
     */
    private void doJoin(Player joiner, Clan clan) {
        UtilServer.callEvent(new MemberJoinClanEvent(joiner, clan));
    }
}
