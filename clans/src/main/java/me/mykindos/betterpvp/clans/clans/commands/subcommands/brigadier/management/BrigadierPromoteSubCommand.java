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
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberPromoteEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierPromoteSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierPromoteSubCommand(ClientManager clientManager, ClanManager clanManager) {
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
        return "promote";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Promotes the specified member";
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
                .then(IBrigadierCommand.argument("Promotable Clan Member",
                                        BPvPClansArgumentTypes.lowerRankClanMember(),
                                    sourceStack -> this.executorHasAClan(sourceStack) && !senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Promotable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);
                            final ClanMember executor = origin.getMember(player.getUniqueId());


                            if (executor.getRank() == ClanMember.MemberRank.LEADER && target.getRank() == ClanMember.MemberRank.ADMIN) {
                                new ConfirmationMenu("Are you sure you want to promote " + target.getClientName() + " to leader?", success -> {
                                    if (success) {
                                        UtilServer.callEvent(new MemberDemoteEvent(player, origin, executor));
                                        UtilServer.callEvent(new MemberPromoteEvent(player, origin, target));
                                    }
                                }).show(player);
                                return Command.SINGLE_SUCCESS;
                            }

                            doPromote(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                //allow administrating clients to promote anyone
                ).then(IBrigadierCommand.argument("Admin Promotable Clan Member",
                                BPvPClansArgumentTypes.clanMember(),
                                sourceStack -> this.executorHasAClan(sourceStack) && senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Admin Promotable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

                            doPromote(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /**
     *
     * @param promoter the player promoting
     * @param clan the clan that this promotion is happening in
     * @param toPromote the member to promote
     */
    private void doPromote(Player promoter, Clan clan, ClanMember toPromote) {
        UtilServer.callEvent(new MemberPromoteEvent(promoter, clan, toPromote));
        SoundEffect.HIGH_PITCH_PLING.play(promoter);
    }
}
