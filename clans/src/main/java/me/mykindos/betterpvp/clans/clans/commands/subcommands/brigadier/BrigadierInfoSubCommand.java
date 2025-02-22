package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import me.mykindos.betterpvp.clans.commands.arguments.types.ClanArgument;
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
        return Commands.literal(getName())
                //by clan name
                .then(Commands.argument("Clan Name", BPvPClansArgumentTypes.clan())
                        .executes(context -> {
                            final Clan target = context.getArgument("Clan Name", Clan.class);
                            if (context.getSource().getExecutor() instanceof final Player player) {
                                final Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                new ClanMenu(player, playerClan, target).show(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                //must be before selector, selector fails without falling to here
                .then(Commands.argument("Offline Clan Member", BPvPArgumentTypes.playerName())
                        .executes(context -> {
                            final String targetName = context.getArgument("Offline Clan Member", String.class);
                            final CommandSender sender = context.getSource().getSender();
                            log.info("offline send").submit();
                            log.info(sender.getName()).submit();
                            getOfflineClientByName(targetName, sender).thenAccept(clientOptional -> {
                                if (clientOptional.isEmpty()) return;
                                final Client targetClient = clientOptional.get();
                                log.info("pre clan").submit();
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

                )
                //by clan member
                //TODO add offline client thing with selector? TODO figure out how to do that (maybe custom player selector?)
                .then(Commands.argument("Clan Member", ArgumentTypes.player())
                        .executes(context -> {
                            final Player target = context.getArgument("Clan Member", PlayerSelectorArgumentResolver.class)
                                    .resolve(context.getSource()).getFirst();
                            final Clan targetClan = clanManager.getClanByPlayer(target)
                                    .orElseThrow(() -> ClanArgument.NOT_IN_A_CLAN_EXCEPTION.create(target.getName()));
                            if (context.getSource().getExecutor() instanceof final Player player) {
                                final Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
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
