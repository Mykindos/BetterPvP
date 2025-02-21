package me.mykindos.betterpvp.core.command.brigadier.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.menu.CachedLogMenu;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@CustomLog
public class BrigadierSearch extends BrigadierCommand {

    private final LogRepository logRepository;
    @Inject
    public BrigadierSearch(ClientManager clientManager, LogRepository logRepository) {
        super(clientManager);
        this.logRepository = logRepository;
    }

    @Override
    public String getName() {
        return "brigadiersearch";
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
        return Commands.literal("brigadiersearch")
                    .then(Commands.argument("Item Id", BPvPArgumentTypes.uuidItem())
                            .executes(context -> {
                                UUIDItem item = context.getArgument("Item Id", UUIDItem.class);
                                if (context.getSource().getExecutor() instanceof Player player) {
                                    new CachedLogMenu(item.getIdentifier(), LogContext.ITEM, item.getUuid().toString(), "ITEM_", CachedLogMenu.ITEM, JavaPlugin.getPlugin(Core.class), logRepository, null).show(player);
                                }
                                return Command.SINGLE_SUCCESS;
                            }
                        )
                    );//TODO by player name
    }
}
