package me.mykindos.betterpvp.clans.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.events.SettingsUpdatedEvent;
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

    private final GamerManager gamerManager;

    @Inject
    public GamerListener(GamerManager gamerManager){
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onClientLogin(ClientLoginEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        if(gamerOptional.isEmpty()){
            Gamer gamer = new Gamer(event.getClient(), event.getClient().getUuid());

            setDefaultProperties(gamer);

            gamerManager.addObject(event.getClient().getUuid(), gamer);
            gamerManager.getGamerRepository().save(gamer);
        }else{
            checkUnsetProperties(gamerOptional.get());
        }

        Bukkit.getPluginManager().callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }

    private void setDefaultProperties(Gamer gamer) {
        gamer.putProperty(GamerProperty.COINS, defaultCoins);
        gamer.putProperty(GamerProperty.FRAGMENTS, defaultFragments);
        gamer.putProperty(GamerProperty.SIDEBAR_ENABLED, true);
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

    @EventHandler
    public void onSettingsUpdated(SettingsUpdatedEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        gamerOptional.ifPresent(gamer -> {
            gamerManager.getGamerRepository().saveProperty(gamer, event.getSetting(), gamer.getProperties().get(event.getSetting()));
        });

        UtilServer.callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }
}
