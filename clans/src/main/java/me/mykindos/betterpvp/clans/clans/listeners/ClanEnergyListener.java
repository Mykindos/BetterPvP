package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.TitleComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;

import java.util.Optional;

public class ClanEnergyListener extends ClanListener{
    @Inject
    @Config(path="clans.energy.energyWarnLevel", defaultValue = "0.1")
    private double energyWarnLevel;
    ClanEnergyListener(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @UpdateEvent(delay = 30 * 1000, isAsync = true)
    public void checkEnergy() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
            if (clanOptional.isPresent()) {
                Clan clan = clanOptional.get();
                if (clan.getEnergyRatio() < 1.0) {
                    Component title = Component.text("CLAN ENERGY LOW", NamedTextColor.RED);
                    Component subTitle = Component.text("Time Remaining: ", NamedTextColor.YELLOW).append(Component.text(clan.getEnergyTimeRemaining(), NamedTextColor.GREEN));
                    player.showTitle(Title.title( title, subTitle));
                    UtilMessage.message(player, "Clans", title);
                    UtilMessage.message(player, "Clans", subTitle);
                }
            }
        });
    }
}

