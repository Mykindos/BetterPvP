package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.clans.logging.menu.ClanKillLogMenu;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierKillLogsCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierKillLogsCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "killlogs";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Get kill logs associated with your clan";
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

                    new ClanKillLogMenu(executorClan, clanManager, clientManager).show(executor);

                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("Clan",
                        BPvPClansArgumentTypes.clan(),
                        this::senderIsAdministrating)
                        .executes(context -> {
                            final Clan target = context.getArgument("Clan", Clan.class);
                            final Player executor = getPlayerFromExecutor(context);
                            new ClanKillLogMenu(target, clanManager, clientManager).show(executor);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && (executorHasAClan(source) || senderIsAdministrating(source));
    }
}
