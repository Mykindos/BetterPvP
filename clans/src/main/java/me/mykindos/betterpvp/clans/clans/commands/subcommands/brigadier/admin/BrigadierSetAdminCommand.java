package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.command.CommandSender;


@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierSetAdminCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierSetAdminCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "setadmin";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Set whether a clan is an admin clan or not";
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
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan())
                        .then(IBrigadierCommand.argument("admin", BPvPArgumentTypes.booleanType())
                                .executes(context -> {
                                    final CommandSender sender = context.getSource().getSender();
                                    final Clan target = context.getArgument("Clan", Clan.class);
                                    final boolean admin = context.getArgument("admin", Boolean.class);

                                    target.setAdmin(admin);
                                    UtilMessage.message(sender, "Clans", "Updated clan admin status to " + admin);
                                    clanManager.getRepository().updateClanAdmin(target);
                                    clientManager.sendMessageToRank("Clans", UtilMessage.deserialize( "<yellow>%s<gray> set <yellow>%s<gray> as an admin clan: <green>%s",
                                            sender.getName(), target.getName(), admin), Rank.ADMIN);

                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                );
    }

}
