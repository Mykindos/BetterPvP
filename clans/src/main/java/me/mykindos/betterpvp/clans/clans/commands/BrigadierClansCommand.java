package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.Optional;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class BrigadierClansCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierClansCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
        this.getAliases().addAll(List.of("c", "f", "faction"));
    }

    @Override
    public String getName() {
        return "clan";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        final LiteralArgumentBuilder<CommandSourceStack> argumentBuilder =  IBrigadierCommand.literal(getName())
                .executes(context -> {
                    final Player executor = getPlayerFromExecutor(context);
                    final Clan executorClan = getClanByExecutor(context);
                    openClanMenu(executor, executorClan, executorClan);
                    return Command.SINGLE_SUCCESS;
                })
                //adding suggestions here will also show all other literal commands regardless of ability to run them
                .then(IBrigadierCommand.argument("Clan Name | Clan Member", StringArgumentType.string())
                    .executes(context -> {
                        final String targetName = context.getArgument("Clan Name | Clan Member", String.class);
                        final Player executor = getPlayerFromExecutor(context);
                        final Clan playerClan = clanManager.getClanByPlayer(executor).orElse(null);
                        final CommandSender sender = context.getSource().getSender();

                        final Optional<Clan> clanNameOptional = clanManager.getClanByName(targetName);
                        //do by clanname first
                        if (clanNameOptional.isPresent()) {
                            final Clan targetClan = clanNameOptional.get();
                            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                                new ClanMenu(executor, playerClan, targetClan).show(executor);
                            });
                            return Command.SINGLE_SUCCESS;
                        }

                        //then by player name
                        getOfflineClientByName(targetName, sender).thenAccept(clientOptional -> {
                            if (clientOptional.isEmpty()) return;
                            final Client targetClient = clientOptional.get();
                            final Optional<Clan> targetClanOptional = getClanByClient(targetClient, sender);
                            if (targetClanOptional.isEmpty()) return;
                            final Clan targetClan = targetClanOptional.get();
                            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                                new ClanMenu(executor, playerClan, targetClan).show(executor);
                            });
                        });
                        return Command.SINGLE_SUCCESS;
                    })
                );

        //doing this will also show all other literal commands regardless of ability to run them
        /*
        //add info pseudo indirect if exists
        final Optional<IBrigadierCommand> infoOptional = getChildren().stream().filter(command -> command.getName().equalsIgnoreCase("info")).findFirst();
        if (infoOptional.isEmpty()) return builder;
        final IBrigadierCommand info = infoOptional.get();
        info.build().getChildren().forEach(child -> {
            builder.then(child.createBuilder());
        });
        */
        return argumentBuilder;
    }

    @Override
    public String getDescription() {
        return "Shows the clan Menu";
    }

    private void openClanMenu(Player player, Clan playerClan, Clan clan) {
        // We swap back to main thread, because the menu is not thread-safe and can lead to players having a bugged inventory (dupe items)
        UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> new ClanMenu(player, playerClan, clan).show(player));
    }
}
