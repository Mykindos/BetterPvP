package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.clans.logging.menu.PlayersOfClanMenu;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import org.bukkit.entity.Player;

@Singleton
@CustomLog
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierGetPlayersOfClanSubCommand extends ClanBrigadierCommand {
    @Inject
    protected BrigadierGetPlayersOfClanSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    public String getName() {
        return "clanplayers";
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
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan())
                        .executes(context -> {
                            final Clan target = context.getArgument("Clan", Clan.class);
                            final Player player = getPlayerFromExecutor(context);
                            new PlayersOfClanMenu(target.getName(), target.getId(), clanManager, clientManager, null).show(player);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    @Override
    public String getDescription() {
        return "Get players associated with a clan";
    }
}
