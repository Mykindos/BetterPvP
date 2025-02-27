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
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierKickSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierKickSubCommand(ClientManager clientManager, ClanManager clanManager) {
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
        return "kick";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Kicks the specified member";
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
                .then(Commands.argument("Kickable Clan Member", BPvPClansArgumentTypes.lowerRankClanMember())
                        .executes(context -> {
                            final String targetName = context.getArgument("Kickable Clan Member", String.class);

                            if (!(context.getSource().getExecutor() instanceof final Player player)) return Command.SINGLE_SUCCESS;

                            final Clan origin = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));

                            final ClanMember executor = origin.getMember(player.getUniqueId());
                            final ClanMember target = origin.getMemberByName(targetName).orElseThrow(() -> ClanArgumentException.MEMBER_NOT_MEMBER_OF_CLAN.create(origin.getName(), targetName));

                            clanManager.targetIsLowerRankThrow(executor, target);

                            //TODO should this be done in a different place?
                            final Player targetPlayer = target.getPlayer();
                            if (targetPlayer != null) {
                                final Optional<Clan> locationClanOptional = clanManager.getClanByLocation(targetPlayer.getLocation());
                                if (locationClanOptional.isPresent()) {
                                    final Clan locationClan = locationClanOptional.get();
                                    if (origin.isEnemy(locationClan)) {
                                        throw ClanArgumentException.TARGET_MEMBER_IN_ENEMY_TERRITORY.create(target.getClientName());
                                    }
                                }
                            }

                            doKick(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
                        //TODO admin kick as separate argument
                        .requires(this::executorHasAClan)
                );
    }

    /**
     *
     * @param kicker the player kicking
     * @param clan the clan that this kick is happening in
     * @param toKick the member to kick
     */
    private void doKick(Player kicker, Clan clan, ClanMember toKick) {
        UtilServer.callEvent(new ClanKickMemberEvent(kicker, clan, toKick));
        SoundEffect.LOW_PITCH_PLING.play(kicker);
    }
}
