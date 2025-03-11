package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanUntrustEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierUntrustCommand extends BrigadierClanSubCommand {
    @Inject
    protected BrigadierUntrustCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "untrust";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "untrusts the specified trusted Ally";
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
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return Commands.literal(getName())
                .then(Commands.argument("Trusted Clan", BPvPClansArgumentTypes.trustedClan())
                        .executes(context -> {
                            final Clan target = context.getArgument("Trusted Clan", Clan.class);
                            final Player executor = getPlayerFromExecutor(context);
                            final Clan executorClan = getClanByExecutor(context);
                            UtilServer.callEvent(new ClanUntrustEvent(executor, executorClan, target));
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(this::executorHasAClan)
                );
    }

}
