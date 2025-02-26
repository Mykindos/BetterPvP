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
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.ClanMemberArgument;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

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
        return Commands.literal(getName())
                .then(Commands.argument("Demotable Clan Member", BPvPClansArgumentTypes.demotableClanMember())
                        .executes(context -> {
                            final String targetName = context.getArgument("Demotable Clan Member", String.class);

                            if (!(context.getSource().getExecutor() instanceof final Player player)) return Command.SINGLE_SUCCESS;

                            final Clan origin = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgument.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

                            final ClanMember executor = origin.getMember(player.getUniqueId());
                            final ClanMember target = origin.getMemberByName(targetName).orElseThrow(() -> ClanMemberArgument.MEMBER_NOT_MEMBER_OF_CLAN.create(origin.getName(), targetName));

                            clanManager.targetIsLowerRankThrow(executor, target);
                            if (target.getRank() == ClanMember.MemberRank.RECRUIT) {
                                throw ClanMemberArgument.TARGET_MEMBER_RANK_TOO_LOW.create(target.getClientName());
                            }

                            doDemote(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        //TODO admin demote as separate argument
                        .requires(this::executorHasAClan)
                );
    }

    /**
     *
     * @param demoter the player demoting
     * @param clan the clan that this demotion is happening in
     * @param toDemote the member to demote
     */
    private void doDemote(Player demoter, Clan clan, ClanMember toDemote) {
        UtilServer.callEvent(new MemberDemoteEvent(demoter, clan, toDemote));
        SoundEffect.LOW_PITCH_PLING.play(demoter);;
    }
}
