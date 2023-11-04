package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class StuckSubCommand extends ClanSubCommand {

    private final CooldownManager cooldownManager;

    @Inject
    public StuckSubCommand(ClanManager clanManager, GamerManager gamerManager, CooldownManager cooldownManager) {
        super(clanManager, gamerManager);
        this.cooldownManager = cooldownManager;
    }

    @Override
    public String getName() {
        return "stuck";
    }

    @Override
    public String getDescription() {
        return "Teleport out of a claim if you are stuck.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!cooldownManager.use(player, getName(), 1000, false, true)){
            UtilMessage.message(player, "Clans", "Try again in a second");
            return;
        }
        
        Optional<Clan> territoryOptional = clanManager.getClanByLocation(player.getLocation());

            if (territoryOptional.isEmpty()) {
                UtilMessage.message(player, "Clans", Component.text("You must be in a claimed territory to use ", NamedTextColor.GRAY)
                        .append(Component.text("/c stuck", NamedTextColor.YELLOW)));
                return;
            }


            Location nearestWilderness = clanManager.closestWilderness(player);

            if (nearestWilderness == null) {
                UtilMessage.message(player, "Clans", Component.text("No wilderness found to teleport to", NamedTextColor.RED));
                return;
            }

            UtilServer.callEvent(new ClanStuckTeleportEvent(player, () -> player.teleport(nearestWilderness)));
    }



    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
