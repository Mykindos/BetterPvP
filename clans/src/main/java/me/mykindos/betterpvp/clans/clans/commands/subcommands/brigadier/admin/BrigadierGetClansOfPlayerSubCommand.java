package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.clans.logging.menu.ClansOfPlayerMenu;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierGetClansOfPlayerSubCommand extends ClanBrigadierCommand {
    @Inject
    protected BrigadierGetClansOfPlayerSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    public String getName() {
        return "playerclans";
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
                //must be before selector, selector fails without falling
                .then(IBrigadierCommand.argument("player", BPvPArgumentTypes.playerName())
                        .suggests(BPvPArgumentTypes.playerName()::suggestions)
                        .executes(context -> {
                            final String targetName = context.getArgument("player", String.class);
                            final Player player = getPlayerFromExecutor(context);
                            getOfflineClientByName(targetName, player).thenAccept(clientOptional -> {
                                if (clientOptional.isEmpty()) return;
                                final Client targetClient = clientOptional.get();
                                ClansOfPlayerMenu clansOfPlayerMenu = new ClansOfPlayerMenu(targetClient, clanManager, clientManager, null);
                                UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                                    clansOfPlayerMenu.show(player);
                                });
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    @Override
    public String getDescription() {
        return "Get clans associated with a player";
    }
}
