package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestTrustEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierTrustCommand extends BrigadierClanSubCommand {
    @Inject
    protected BrigadierTrustCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "trust";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Trusts the specified Ally";
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
        return IBrigadierCommand.literal(getName())
                .then(Commands.argument("Trustable Clan", BPvPClansArgumentTypes.trustableClan())
                        .executes(context -> {
                            final Clan target = context.getArgument("Trustable Clan", Clan.class);
                            if (!(context.getSource().getExecutor() instanceof final Player player)) return Command.SINGLE_SUCCESS;

                            final Clan origin = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

                            doTrust(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        .requires(this::executorHasAClan)
                );
    }

    private void doTrust(Player originPlayer, Clan origin, Clan target) throws CommandSyntaxException {
        clanManager.canTrustThrow(origin, target);
        UtilServer.callEvent(new ClanRequestTrustEvent(originPlayer, origin, target));
    }


}
