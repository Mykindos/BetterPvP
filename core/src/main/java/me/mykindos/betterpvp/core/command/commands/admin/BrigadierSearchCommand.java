package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@CustomLog
public class BrigadierSearchCommand extends BrigadierCommand {

    private final LogRepository logRepository;
    @Inject
    public BrigadierSearchCommand(ClientManager clientManager, LogRepository logRepository) {
        super(clientManager);
        this.logRepository = logRepository;
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "a command to test brigadier implementation using the /search command";
    }

    /**
     * Define the command, using normal rank based permissions
     *
     * @return the builder for the command
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal("search")
                    .then(IBrigadierCommand.argument("Item Id", BPvPArgumentTypes.uuidItem())
                            .executes(context -> {
                                final UUIDItem item = context.getArgument("Item Id", UUIDItem.class);
                                final Player executor = getPlayerFromExecutor(context);
                                new CachedLogMenu(
                                        item.getIdentifier(),
                                        LogContext.ITEM,
                                        item.getUuid().toString(),
                                        "ITEM_",
                                        CachedLogMenu.ITEM,
                                        JavaPlugin.getPlugin(Core.class),
                                        logRepository,
                                        null)
                                            .show(executor);
                                return Command.SINGLE_SUCCESS;
                            }
                        )
                    ).then(IBrigadierCommand.argument("Player Name", BPvPArgumentTypes.playerName())
                        .suggests(BPvPArgumentTypes.playerName()::suggestions)
                        .executes(context -> {
                            final String targetName = context.getArgument("Clan Member", String.class);
                            final Player executor = getPlayerFromExecutor(context);
                            getOfflineClientByName(targetName, executor).thenAccept(clientOptional -> {
                                if (clientOptional.isEmpty()) return;
                                final Client targetClient = clientOptional.get();
                                CachedLogMenu cachedLogMenu = new CachedLogMenu(
                                        targetClient.getName(),
                                        LogContext.CLIENT,
                                        targetClient.getUniqueId().toString(),
                                        "ITEM_",
                                        CachedLogMenu.ITEM,
                                        JavaPlugin.getPlugin(Core.class),
                                        logRepository,
                                        null);
                                UtilServer.runTask(JavaPlugin.getPlugin(Core.class), () -> {
                                    cachedLogMenu.show(executor);
                                });
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}
