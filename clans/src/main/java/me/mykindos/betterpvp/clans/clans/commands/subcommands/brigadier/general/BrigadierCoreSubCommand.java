package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanCoreTeleportEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierCoreSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierCoreSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
        this.getAliases().add("home");
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.RECRUIT;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "core";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Teleports to your clan core";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> define() {
        return IBrigadierCommand.literal(getName())
                .executes(context -> {

                    final Player executor = getPlayerFromExecutor(context);
                    final Clan executorClan = getClanByExecutor(context);

                    ClanCore core = executorClan.getCore();
                    if (!core.isSet()) {
                        throw ClanArgumentException.NO_CORE_SET.create(executorClan);
                    }

                    UtilServer.callEvent(new ClanCoreTeleportEvent(executor, () -> core.teleport(executor, true)));
                    return Command.SINGLE_SUCCESS;
                })
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan(), this::senderIsAdministrating)
                        .executes(context -> {
                            final Player executor = getPlayerFromExecutor(context);
                            final Clan targetClan = context.getArgument("Clan", Clan.class);
                            ClanCore core = targetClan.getCore();
                            if (!core.isSet()) {
                                throw ClanArgumentException.NO_CORE_SET.create(targetClan);
                            }
                            core.teleport(executor, false);
                            return Command.SINGLE_SUCCESS;
                        })

                );
    }
}
