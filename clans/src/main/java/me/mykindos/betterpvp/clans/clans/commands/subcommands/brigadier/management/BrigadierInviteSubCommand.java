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
import me.mykindos.betterpvp.clans.clans.events.ClanInviteMemberEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierInviteSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierInviteSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    @Override
    protected ClanMember.MemberRank requiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "invite";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Invites the specified player";
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
                .then(Commands.argument("Invitable Player", BPvPClansArgumentTypes.invitablePlayer())
                        .executes(context -> {
                            final Player target = context.getArgument("Invitable Player", Player.class);

                            if (!(context.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;

                            final Clan origin = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

                            final Client executorClient = clientManager.search().online(player);
                            final Client targetClient = clientManager.search().online(target);

                            clanManager.canInviteToClan(executorClient, targetClient);

                            doInvite(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        //TODO admin invite as separate argument
                        .requires(this::executorHasAClan)
                );
    }

    /**
     *
     * @param inviter the player inviting
     * @param clan the clan that this invitation is happening in
     * @param toInvite the member to invite
     */
    private void doInvite(Player inviter, Clan clan, Player toInvite) {
        UtilServer.callEvent(new ClanInviteMemberEvent(inviter, clan, toInvite));
    }
}
