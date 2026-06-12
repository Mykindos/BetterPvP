package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.delayedactions.events.ClanStuckTeleportEvent;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class StuckSubCommand extends ClanSubCommand {

    private final CooldownManager cooldownManager;
    private final ZoneManager zoneManager;

    @Inject
    public StuckSubCommand(ClanManager clanManager, ClientManager clientManager, CooldownManager cooldownManager, ZoneManager zoneManager) {
        super(clanManager, clientManager);
        this.cooldownManager = cooldownManager;
        this.zoneManager = zoneManager;
    }

    @Override
    public String getName() {
        return "stuck";
    }

    @Override
    public String getDescription() {
        return "clans.command.stuck.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(!cooldownManager.use(player, getName(), 5, false, true)){
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.stuck.cooldown");
            return;
        }

        Zone zone = zoneManager.getZone(player);
        if (zone == null) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.stuck.not-claimed");
            return;
        }


        Optional<Location> nearestWilderness = clanManager.closestWilderness(player);
        if (nearestWilderness.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.stuck.no-wilderness");
            return;
        }

        UtilServer.callEvent(new ClanStuckTeleportEvent(player, () -> player.teleport(nearestWilderness.get())));
    }



    @Override
    public boolean canExecuteWithoutClan() {
        return true;
    }
}
