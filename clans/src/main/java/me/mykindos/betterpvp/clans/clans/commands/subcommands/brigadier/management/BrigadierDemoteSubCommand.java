package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierDemoteSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierDemoteSubCommand(ClientManager clientManager, ClanManager clanManager) {
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
        return "demote";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Demotes the specified member";
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
                .then(IBrigadierCommand.argument("Demotable Clan Member",
                                BPvPClansArgumentTypes.demotableClanMember(),
                                sourceStack -> this.executorHasAClan(sourceStack) && !senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Demotable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

                            doDemote(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        //allow administrating clients to demote anyone
                ).then(IBrigadierCommand.argument("Admin Demotable Clan Member",
                        BPvPClansArgumentTypes.clanMember(),
                        sourceStack -> this.executorHasAClan(sourceStack) && senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Admin Demotable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

                            doDemote(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                );
    }

    /**
     *
     * @param demoter the player demoting
     * @param clan the clan that this demotion is happening in
     * @param toDemote the member to demote
     */
    private void doDemote(@NotNull final Player demoter, @NotNull final Clan clan, @NotNull final ClanMember toDemote) throws CommandSyntaxException {
        if (toDemote.getRank() == ClanMember.MemberRank.RECRUIT) {
            throw ClanArgumentException.TARGET_MEMBER_RANK_TOO_LOW.create(toDemote.getClientName());
        }
        UtilServer.callEvent(new MemberDemoteEvent(demoter, clan, toDemote));
        SoundEffect.LOW_PITCH_PLING.play(demoter);
    }
}
