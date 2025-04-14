package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;


@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierSetExpCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierSetExpCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "setexp";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Force set a clan's experience";
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
                        .then(IBrigadierCommand.argument("experience", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {
                                    final CommandSender sender = context.getSource().getSender();
                                    final Clan target = context.getArgument("Clan", Clan.class);
                                    final double experience = context.getArgument("experience", Double.class);

                                    double prevExperience = target.getExperience();
                                    long prevLevel = target.getLevel();
                                    target.setExperience(experience);
                                    double newExperience = target.getExperience();
                                    long newLevel = target.getLevel();

                                    UtilMessage.message(sender,
                                            "Clans",
                                            "Set clan <alt>%s</alt>'s experience from <alt2>%,.1f (level %,d)</alt2> to <alt2>%,.1f (level %,d)</alt2>.",
                                            target.getName(),
                                            prevExperience,
                                            prevLevel,
                                            newExperience,
                                            newLevel);


                                    final Component alert = UtilMessage.deserialize("%s set clan <alt>%s</alt>'s experience from <alt2>%,.1f (level %,d)</alt2> to <alt2>%,.1f (level %,d)</alt2>.",
                                            sender.getName(),
                                            target.getName(),
                                            prevExperience,
                                            prevLevel,
                                            newExperience,
                                            newLevel);
                                    clientManager.sendMessageToRank("Clans", alert, Rank.HELPER);

                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                );
    }

}
