package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierInfoSubCommand extends BrigadierCommand {

    private final ClanManager clanManager;
    @Inject
    protected BrigadierInfoSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager);
        this.clanManager = clanManager;
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
                .then(Commands.argument("Offline Clan Member", BPvPArgumentTypes.OfflineClient)
                        .executes(context -> {
                            Client targetClient = context.getArgument("Offline Clan Member", Client.class);
                            Clan targetClan = clanManager.getClanByPlayer(targetClient.getUniqueId())
                                    .orElseThrow(() -> BPvPClansArgumentTypes.NOTINACLANEXCEPTION.create(targetClient.getName()));
                            if (context.getSource().getExecutor() instanceof Player player) {
                                Clan playerClan = clanManager.getClanByPlayer(player).orElse(null);
                                new ClanMenu(player, playerClan, targetClan).show(player);
                            }
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
                                    .orElseThrow(() -> BPvPClansArgumentTypes.NOTINACLANEXCEPTION.create(target.getName()));
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
