package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.BPvPClansArgumentTypes;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import org.bukkit.entity.Player;


@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierSetDominanceCommand extends ClanBrigadierCommand {

    @Inject
    protected BrigadierSetDominanceCommand(ClientManager clientManager, ClanManager clanManager) {
        super(clientManager, clanManager);
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "setdominance";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "sets the dominance of the current clan against the target clan";
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
                .then(Commands.argument("Target Clan", BPvPClansArgumentTypes.enemyClan())
                        //do it by clan you are in
                        .then(Commands.argument("Dominance", DoubleArgumentType.doubleArg(-100.0, 100.0))
                                .executes(context -> {
                                    Clan targetClan = context.getArgument("Target Clan", Clan.class);
                                    double newDominance = context.getArgument("Dominance", double.class);
                                    if (!(context.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;

                                    Clan originClan = clanManager.getClanByPlayer(player).orElseThrow(() -> ClanArgumentException.NOT_IN_A_CLAN_EXCEPTION.create(player.getName()));
                                    applyNewDominance(targetClan, originClan, newDominance);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .requires(this::executorHasAClan)
                        )
                        //TODO remove. Cannot do it this way because this clashes with hints for Dominance argument
                        /*
                        //or by not in a clan
                        .then(Commands.argument("Origin Clan", BPvPClansArgumentTypes.CLAN)
                                .then(Commands.argument("New Dominance", DoubleArgumentType.doubleArg(-100.0, 100.0))
                                        .executes(context -> {
                                            Clan targetClan = context.getArgument("Target Clan", Clan.class);
                                            double newDominance = context.getArgument("New Dominance", double.class);
                                            Clan originClan = context.getArgument("Origin Clan", Clan.class);
                                            applyNewDominance(targetClan, originClan, newDominance);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )*/
                );
    }

    private void applyNewDominance(Clan target, Clan origin, double newDominance) throws CommandSyntaxException {
        //the clan the DOM will be applied to
        final Clan applyClan = newDominance > 0 ? target : origin;
        //the Clan the DOM will be zeroed to
        final Clan zeroClan = newDominance > 0 ? origin : target;

        final double actualDominance = Math.abs(newDominance);

        final ClanEnemy applyClanEnemy = applyClan.getEnemy(zeroClan).orElseThrow(() -> ClanArgumentException.CLAN_NOT_ENEMY_OF_CLAN.create(applyClan, zeroClan));
        final ClanEnemy zeroClanEnemy = zeroClan.getEnemy(applyClan).orElseThrow(() -> ClanArgumentException.CLAN_NOT_ENEMY_OF_CLAN.create(zeroClan, applyClan));

        applyClanEnemy.setDominance(actualDominance);
        zeroClanEnemy.setDominance(0);

        clanManager.getRepository().updateDominance(applyClan, applyClanEnemy);
        clanManager.getRepository().updateDominance(zeroClan, zeroClanEnemy);

        zeroClan.messageClan("<gray>Your dominance against <red>" + applyClan.getName()
                + " <gray>has been set to <green>" + actualDominance + "%", null, true);
        applyClan.messageClan("<gray>Your dominance against <red>" + zeroClan.getName()
                + " <gray>has been set to <red>-" + actualDominance + "%", null, true);

    }


}
