package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.management;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

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
        return IBrigadierCommand.literal(getName())
                .then(IBrigadierCommand.argument("Kickable Clan Member",
                                BPvPClansArgumentTypes.lowerRankClanMember(),
                                sourceStack -> this.executorHasAClan(sourceStack) && !this.senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Kickable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

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
                //allow administrating clients to kick any member
                ).then(IBrigadierCommand.argument("Admin Kickable Clan Member",
                                BPvPClansArgumentTypes.clanMember(),
                                sourceStack -> this.executorHasAClan(sourceStack) && senderIsAdministrating(sourceStack))
                        .executes(context -> {
                            final ClanMember target = context.getArgument("Admin Kickable Clan Member", ClanMember.class);

                            final Player player = getPlayerFromExecutor(context);
                            final Clan origin = getClanByExecutor(context);

                            doKick(player, origin, target);
                            return Command.SINGLE_SUCCESS;
                        })
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
