package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Objects;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.BrigadierClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierLeaveSubCommand extends BrigadierClanSubCommand {

    @Inject
    protected BrigadierLeaveSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
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
        return "leave";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Leaves your current clan";
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

                    final Optional<ClanMember> leaderOptional = executorClan.getLeader();
                    //skip leader check if sender is administrating, there is no leader, or this is an admin clan
                    if (!senderIsAdministrating(context.getSource())
                            && leaderOptional.isPresent()
                            && !executorClan.isAdmin()) {
                        final ClanMember leader = leaderOptional.get();
                        if (leader.equals(executorClan.getMember(executor.getUniqueId()))) {
                            throw ClanArgumentException.LEADER_CANNOT_LEAVE.create();
                        }
                    }

                    final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(executor.getLocation());
                    if (locationClanOptional.isPresent()) {
                        final Clan locationClan = locationClanOptional.get();
                        if (executorClan.isEnemy(locationClan)) {
                            throw ClanArgumentException.CANNOT_LEAVE_IN_ENEMY_TERRITORY.create();
                        }
                    }

                    new ConfirmationMenu("Are you sure you want to leave your clan?", success -> {
                        if (success) {
                            UtilServer.callEvent(new MemberLeaveClanEvent(executor, executorClan));
                        }
                    }).show(executor);
                    return Command.SINGLE_SUCCESS;
                });
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        boolean passesPrevious = super.requirement(source);
        if (!passesPrevious) return false;

        //allow administrating clients to run command
        if (senderIsAdministrating(source)) return true;

        if (!(source.getSender() instanceof final Player player)) return false;

        //prevent normal clan leaders from leaving their clans
        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if (clanOptional.isEmpty()) return false;
        final Clan clan = clanOptional.get();

        return !clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.LEADER);
    }

    @Override
    public Component getRequirementComponent(CommandContext<CommandSourceStack> context) {
        Component component = super.getRequirementComponent(context);
        final Optional<Clan> clanOptional = clanManager.getClanByPlayer(Objects.requireNonNull(context.getSource().getExecutor()).getUniqueId());
        final boolean inClan = clanOptional.isPresent();
        boolean hasClanRank = false;
        if (clanOptional.isPresent()) {
            final Clan clan = clanOptional.get();
            final ClanMember.MemberRank rank = clan.getMember(Objects.requireNonNull(context.getSource().getExecutor()).getUniqueId()).getRank();
            if (!rank.hasRank(ClanMember.MemberRank.LEADER)) {
                hasClanRank = rank.hasRank(this.requiredMemberRank());
            }
        }
        component = component.appendNewline();
        component = component.append(Component.text("Need a Clan: ", NamedTextColor.WHITE))
                .append(Component.text(true, NamedTextColor.GREEN).append(Component.text(" | ", NamedTextColor.GRAY))
                        .append((Component.text("You: ", NamedTextColor.WHITE))
                                .append(Component.text(inClan, inClan? NamedTextColor.GREEN : NamedTextColor.RED))));
        component = component.appendNewline();
        component = component.append(Component.text("Clan Rank: ", NamedTextColor.WHITE))
                .append(Component.text(requiredMemberRank().name(), NamedTextColor.GREEN).append(Component.text(" | ", NamedTextColor.GRAY))
                        .append((Component.text("You Qualify: ", NamedTextColor.WHITE))
                                .append(Component.text(hasClanRank, hasClanRank ? NamedTextColor.GREEN : NamedTextColor.RED))));
        component = component.appendNewline();
        component = component.append(UtilMessage.deserialize("<white>You cannot use this command as a <aqua>Clan</aqua> <light_purple>%s</light_purple>", ClanMember.MemberRank.LEADER.name()));
        return component;
    }
}
