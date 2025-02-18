package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.ClanArgument;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.utilities.UtilServer;
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
        return Commands.literal("info")
                //by clan name
                .then(Commands.argument("Clan Name", BPvPClansArgumentTypes.CLAN)
                        .executes(context -> {
                            Clan target = context.getArgument("Clan Name", Clan.class);
                            if (context.getSource().getExecutor() instanceof Player player) {
                                Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                new ClanMenu(player, playerClan, target).show(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                //must be before selector
                .then(Commands.argument("Offline Clan Member", BPvPArgumentTypes.PlayerName)
                        .executes(context -> {
                            String targetName = context.getArgument("Offline Clan Member", String.class);
                            CommandSender sender = context.getSource().getSender();
                            log.info("offline send").submit();
                            log.info(sender.getName()).submit();
                            getOfflineClientByName(targetName, sender).thenAccept(clientOptional -> {
                                if (clientOptional.isEmpty()) return;
                                Client targetClient = clientOptional.get();
                                log.info("pre clan").submit();
                                Optional<Clan> targetClanOptional = getClanByClient(targetClient, sender);
                                try {
                                    targetClanOptional.orElseThrow(() -> ClanArgument.NOTINACLANEXCEPTION.create("name"));
                                } catch (CommandSyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                                if (targetClanOptional.isEmpty()) return;
                                Clan targetClan = targetClanOptional.get();
                                if (context.getSource().getExecutor() instanceof Player player) {
                                    Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                    UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                                        new ClanMenu(player, playerClan, targetClan).show(player);
                                    });
                                }
                            }).exceptionally(throwable -> {
                                throw ClanArgument.NOTINACLANEXCEPTION.create("name");
                            });
                            return Command.SINGLE_SUCCESS;
                        })

                )
                //by clan member
                //TODO add offline client thing with selector?
                .then(Commands.argument("Clan Member", ArgumentTypes.player())
                        .executes(context -> {
                            Player target = context.getArgument("Clan Member", PlayerSelectorArgumentResolver.class)
                                    .resolve(context.getSource()).getFirst();
                            Clan targetClan = clanManager.getClanByPlayer(target)
                                    .orElseThrow(() -> ClanArgument.NOTINACLANEXCEPTION.create(target.getName()));
                            if (context.getSource().getExecutor() instanceof Player player) {
                                Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                new ClanMenu(player, playerClan, targetClan).show(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                //TODO by offline player name
                ;
    }


    @Override
    public String getDescription() {
        return "Get information about the specified clan";
    }
}
