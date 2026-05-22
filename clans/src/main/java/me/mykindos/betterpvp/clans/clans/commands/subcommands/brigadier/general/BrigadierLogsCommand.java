package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierLogsCommand extends ClanBrigadierCommand {
    private final LogRepository logRepository;

    @Inject
    protected BrigadierLogsCommand(ClientManager clientManager, ClanManager clanManager, LogRepository logRepository) {
        super(clientManager, clanManager);
        this.logRepository = logRepository;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "logs";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Get logs associated with your clan";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .executes(context -> {
                    final Player executor = getPlayerFromExecutor(context);
                    final Clan executorClan = getClanByExecutor(context);

                    new CachedLogMenu(executorClan.getName(),
                            LogContext.CLAN,
                            Long.toString(executorClan.getId()),
                            null,
                            CachedLogMenu.CLANS,
                            JavaPlugin.getPlugin(Clans.class),
                            logRepository,
                            null)
                                .show(executor);

                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("Clan",
                        BPvPClansArgumentTypes.clan(),
                        this::senderIsAdministrating)
                        .executes(context -> {
                            final Clan target = context.getArgument("Clan", Clan.class);
                            final Player executor = getPlayerFromExecutor(context);
                            new CachedLogMenu(target.getName(),
                                    LogContext.CLAN,
                                    Long.toString(target.getId()),
                                    null,
                                    CachedLogMenu.CLANS,
                                    JavaPlugin.getPlugin(Clans.class),
                                    logRepository,
                                    null)
                                    .show(executor);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && (executorHasAClan(source) || senderIsAdministrating(source));
    }
}
