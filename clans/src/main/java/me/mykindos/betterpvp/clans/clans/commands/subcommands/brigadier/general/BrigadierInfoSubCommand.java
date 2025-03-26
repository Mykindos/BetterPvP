package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierInfoSubCommand extends ClanBrigadierCommand {
    @Inject
    protected BrigadierInfoSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    public String getName() {
        return "info";
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
                //by clan name
                .then(IBrigadierCommand.argument("Clan Name", BPvPClansArgumentTypes.clan())
                        .suggests((context, builder) -> {
                            /*
                            we need to do it this way for reasons. I have no idea why. If BPvPArgumentTypes.playerName()
                            has suggestions, no suggestions will show for the client. So instead we add them to the suggestions
                            here. Command still works as intended.
                            */
                            Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().contains(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return BPvPClansArgumentTypes.clan().listSuggestions(context, builder);
                        })
                        .executes(context -> {
                            final Clan target = context.getArgument("Clan Name", Clan.class);
                            if (context.getSource().getExecutor() instanceof final Player player) {
                                final Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                new ClanMenu(player, playerClan, target).show(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                //must be before selector, selector fails without falling
                .then(IBrigadierCommand.argument("Clan Member", BPvPArgumentTypes.playerName())
                        .suggests((context, builder) -> Suggestions.empty())
                        .executes(context -> {
                            final String targetName = context.getArgument("Clan Member", String.class);
                            final CommandSender sender = context.getSource().getSender();
                            getOfflineClientByName(targetName, sender).thenAccept(clientOptional -> {
                                if (clientOptional.isEmpty()) return;
                                final Client targetClient = clientOptional.get();
                                final Optional<Clan> targetClanOptional = getClanByClient(targetClient, sender);
                                if (targetClanOptional.isEmpty()) return;
                                final Clan targetClan = targetClanOptional.get();
                                if (context.getSource().getExecutor() instanceof final Player player) {
                                    final Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                    UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                                        new ClanMenu(player, playerClan, targetClan).show(player);
                                    });
                                }
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    @Override
    public String getDescription() {
        return "Get information about the specified clan";
    }
}
