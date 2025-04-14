package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierEnemyCommand extends BrigadierClanSubCommand {
    @Inject
    protected BrigadierEnemyCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "enemy";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "enemies the specified Clan";
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
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
                .then(Commands.argument("Enemyable Clan", BPvPClansArgumentTypes.enemyableClan())
                        .executes(context -> {
                            final Clan target = context.getArgument("Enemyable Clan", Clan.class);
                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

                            doEnemy(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(this::executorHasAClan)
                );
    }

    private void doEnemy(Player originPlayer, Clan origin, Clan target) throws CommandSyntaxException {
        clanManager.canEnemyThrow(origin, target);
        UtilServer.callEvent(new ClanEnemyEvent(originPlayer, origin, target));
    }


}
