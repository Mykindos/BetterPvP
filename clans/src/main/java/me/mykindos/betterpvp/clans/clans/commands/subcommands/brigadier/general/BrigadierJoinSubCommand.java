package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
@CustomLog
public class BrigadierJoinSubCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierJoinSubCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "join";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Joins the specified clan";
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
                //todo cleanup, then a no suggestion clan argument, because reasons
                //todo trace where suggestions get applied
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan(), this::senderIsAdministrating)
                                .executes(context -> {
                                    final Clan target = context.getArgument("Clan", Clan.class);
                                    if (!(context.getSource().getExecutor() instanceof final Player player)) return Command.SINGLE_SUCCESS;
                                    if (clanManager.getClanByPlayer(player).isPresent()) throw ClanArgumentException.MUST_NOT_BE_IN_A_CLAN_EXCEPTION.create(player.getName());

                                    final Component notification = UtilMessage.deserialize("<yellow>%s</yellow> had <yellow>%s</yellow> force join <aqua>%s</aqua>",
                                            context.getSource().getSender().getName(), player.getName(), target.getName());
                                    clientManager.sendMessageToRank("Clans", notification, Rank.HELPER);

                                    doJoin(player, target);
                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .then(IBrigadierCommand.argument("Joinable Clan", BPvPClansArgumentTypes.joinableClan(),
                                        (source) -> !this.executorHasAClan(source) && !this.senderIsAdministrating(source))
                                .executes(context -> {
                                    final Clan target = context.getArgument("Joinable Clan", Clan.class);

                                    if (!(context.getSource().getExecutor() instanceof final Player player)) return Command.SINGLE_SUCCESS;

                                    final Client client = getClientFromExecutor(context);

                                    clanManager.canJoinClan(client, target);

                                    doJoin(player, target);
                                    return Command.SINGLE_SUCCESS;
                                })
                        //TODO admin join as separate argument


                );
    }

    @Override
    public boolean requirement(CommandSourceStack source) {
        return super.requirement(source) && !executorHasAClan(source);
    }

    @Override
    public Component getRequirementComponent(CommandContext<CommandSourceStack> context) {
        Component component = super.getRequirementComponent(context);
        boolean inClan = executorHasAClan(context.getSource());
        component = component.appendNewline();
        component = component.append(Component.text("Need a Clan: ", NamedTextColor.WHITE))
                .append(Component.text(false, NamedTextColor.RED).append(Component.text(" | ", NamedTextColor.GRAY))
                        .append((Component.text("You: ", NamedTextColor.WHITE))
                                .append(Component.text(inClan, inClan ? NamedTextColor.GREEN : NamedTextColor.RED))));
        return component;
    }

    /**
     *
     * @param joiner the player joining
     * @param clan the clan that this join is happening in
     */
    private void doJoin(Player joiner, Clan clan) {
        UtilServer.callEvent(new MemberJoinClanEvent(joiner, clan));
    }
}
