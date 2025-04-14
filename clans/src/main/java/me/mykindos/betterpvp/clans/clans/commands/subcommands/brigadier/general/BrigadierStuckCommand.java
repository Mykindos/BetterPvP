package me.mykindos.betterpvp.clans.clans.commands.subcommands.brigadier.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.BrigadierClansCommand;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.commands.commands.ClanBrigadierCommand;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.BrigadierSubCommand;
import me.mykindos.betterpvp.core.command.brigadier.IBrigadierCommand;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Singleton
@BrigadierSubCommand(BrigadierClansCommand.class)
public class BrigadierStuckCommand extends ClanBrigadierCommand {
    private final CooldownManager cooldownManager;

    @Inject
    protected BrigadierStuckCommand(ClientManager clientManager, ClanManager clanManager, CooldownManager cooldownManager) {
        super(clientManager, clanManager);
        this.cooldownManager = cooldownManager;
    }

    /**
     * Used in retrieving path for config options
     *
     * @return the name of this command
     */
    @Override
    public String getName() {
        return "stuck";
    }

    /**
     * Gets the description of this command, used in registration
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return "Teleport out of a claim if you are stuck";
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
                    Player executor = getPlayerFromExecutor(context);

                    if (!cooldownManager.use(executor, getName(), 5, false, true)) {
                        throw ArgumentException.COMMAND_ON_COOLDOWN.create(cooldownManager.getAbilityRecharge(executor, getName()).getRemaining());
                    }

                    Optional<Clan> territoryOptional = clanManager.getClanByLocation(executor.getLocation());
                    if (territoryOptional.isEmpty()) {
                        throw ClanArgumentException.NOT_ON_CLAIMED_TERRITORY.create();
                    }

                    Location nearestWilderness = clanManager.closestWilderness(executor);

                    if (nearestWilderness == null) {
                        throw ClanArgumentException.NO_NEARBY_WILDERNESS.create();
                    }

                    UtilServer.callEvent(new ClanStuckTeleportEvent(executor, () -> executor.teleportAsync(nearestWilderness)));

                    return Command.SINGLE_SUCCESS;
                });
    }


}
