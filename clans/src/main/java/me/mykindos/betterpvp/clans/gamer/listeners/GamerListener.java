package me.mykindos.betterpvp.clans.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class GamerListener implements Listener {

    @Inject
    @Config(path="gamer.default.coins", defaultValue = "5000")
    private int defaultCoins;

    @Inject
    @Config(path="gamer.default.fragments", defaultValue = "0")
    private int defaultFragments;

    private final Clans clans;
    private final GamerManager gamerManager;

    @Inject
    public GamerListener(Clans clans, GamerManager gamerManager){
        this.clans = clans;
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {

        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        Gamer gamer;
        if(gamerOptional.isEmpty()){
            gamer = new Gamer(event.getClient(), event.getClient().getUuid());

            gamerManager.addObject(event.getClient().getUuid(), gamer);
            gamerManager.getGamerRepository().save(gamer);
            gamerManager.getBuildRepository().loadDefaultBuilds(gamer);
        }else{
            gamer = gamerOptional.get();
            gamerManager.getBuildRepository().loadBuilds(gamer);
            gamerManager.getBuildRepository().loadDefaultBuilds(gamer);

        }
        checkUnsetProperties(gamer);

        Bukkit.getOnlinePlayers().forEach(player -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)));

    }

    private void checkUnsetProperties(Gamer gamer) {
        Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.COINS);
        if(coinsOptional.isEmpty()){
            gamer.putProperty(GamerProperty.COINS, defaultCoins);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.COINS, defaultCoins);
        }

        Optional<Integer> fragmentsOptional = gamer.getProperty(GamerProperty.FRAGMENTS);
        if(fragmentsOptional.isEmpty()){
            gamer.putProperty(GamerProperty.FRAGMENTS, defaultFragments);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.FRAGMENTS, defaultFragments);
        }

        Optional<Boolean> sidebarOptional = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED);
        if(sidebarOptional.isEmpty()){
            gamer.putProperty(GamerProperty.SIDEBAR_ENABLED, true);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.SIDEBAR_ENABLED, true);
        }
    }

}
