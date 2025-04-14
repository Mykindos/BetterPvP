package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.impl.BPvPLiteralArgumentBuilder;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;


@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierSetEnergyCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierSetEnergyCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "setenergy";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Force sets the energy for your current clan";
    }

    /**
     * Define the command, using normal rank based permissions
     * Requires sender to have required rank and executor to be a player
     *
     * @return the builder to be used in Build
     */
    @Override
    public BPvPLiteralArgumentBuilder define() {
        return IBrigadierCommand.literal(getName())
                .then(IBrigadierCommand.argument("Clan", BPvPClansArgumentTypes.clan())
                        .then(IBrigadierCommand.argument("energy", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    final CommandSender sender = context.getSource().getSender();
                                    final Clan target = context.getArgument("Clan", Clan.class);
                                    final int energy = context.getArgument("energy", Integer.class);

                                    int oldEnergy = target.getEnergy();
                                    target.setEnergy(energy);

                                    Component component = Component.text("Energy set to: ", NamedTextColor.GRAY)
                                            .append(Component.text(target.getEnergy() + " - (", NamedTextColor.YELLOW)
                                                    .append(Component.text(target.getEnergyTimeRemaining(), NamedTextColor.GOLD)
                                                            .append(Component.text(")", NamedTextColor.YELLOW))))
                                            .append(Component.text(" Previous: ", NamedTextColor.GRAY)).append(Component.text(oldEnergy, NamedTextColor.YELLOW));

                                    UtilMessage.message(sender, "Clans", component);
                                    clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> set the energy of <yellow>%s<gray> to <green>%s <white>(<yellow>%s<white>)",
                                            sender.getName(), target.getName(), target.getEnergy(), target.getEnergyTimeRemaining()), Rank.HELPER);

                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                );
    }

}
