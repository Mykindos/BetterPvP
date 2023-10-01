package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.EnergyCheckEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
public class ClanEnergyListener extends ClanListener{

    private final Clans clans;


    @Inject
    @Config(path="clans.energy.energyWarnLevel", defaultValue = "4.0")
    private double energyWarnLevel;

    @Inject
    ClanEnergyListener(Clans clans, ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
    }

    @UpdateEvent(delay = 30 * 1000, isAsync = true)
    public void checkEnergy() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                UtilServer.runTaskLater(clans, () -> {UtilServer.callEvent(new EnergyCheckEvent(player, clan));}, 5);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnergyCheck(EnergyCheckEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Clan clan = event.getClan();
        if (clan.getEnergyRatio() < 36.0) {
            Component title = Component.text("CLAN ENERGY LOW", NamedTextColor.RED);
            Component subTitle = Component.text("Time Remaining: ", NamedTextColor.YELLOW).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN));
            player.showTitle(Title.title( title, subTitle));
            UtilMessage.message(player, "Clans", title);
            UtilMessage.message(player, "Clans", subTitle);
        }
    }
}

