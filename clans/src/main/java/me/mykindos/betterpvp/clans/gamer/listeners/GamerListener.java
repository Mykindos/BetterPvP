package me.mykindos.betterpvp.clans.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.events.SettingsUpdatedEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
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
        Gamer gamer;
        if(gamerOptional.isEmpty()){
            gamer = new Gamer(event.getClient(), event.getClient().getUuid());

            gamerManager.addObject(event.getClient().getUuid(), gamer);
            gamerManager.getGamerRepository().save(gamer);
        }else{
            gamer = gamerOptional.get();

        }
        checkUnsetProperties(gamer);
        Bukkit.getPluginManager().callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }


    private void checkUnsetProperties(Gamer gamer) {
        Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.COINS.toString());
        if(coinsOptional.isEmpty()){
            gamer.putProperty(GamerProperty.COINS.toString(), defaultCoins);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.COINS.toString(), defaultCoins);
        }

        Optional<Integer> fragmentsOptional = gamer.getProperty(GamerProperty.FRAGMENTS.toString());
        if(fragmentsOptional.isEmpty()){
            gamer.putProperty(GamerProperty.FRAGMENTS.toString(), defaultFragments);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.FRAGMENTS.toString(), defaultFragments);
        }

        Optional<Boolean> sidebarOptional = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED.toString());
        if(sidebarOptional.isEmpty()){
            gamer.putProperty(GamerProperty.SIDEBAR_ENABLED.toString(), true);
            gamerManager.getGamerRepository().saveProperty(gamer, GamerProperty.SIDEBAR_ENABLED.toString(), true);
        }
    }

    @EventHandler
    public void onSettingsUpdated(SettingsUpdatedEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        gamerOptional.ifPresent(gamer -> {
            if(Arrays.stream(GamerProperty.values()).anyMatch(prop -> prop.getKey().equalsIgnoreCase(event.getSetting()))) {
                gamerManager.getGamerRepository().saveProperty(gamer, event.getSetting(), gamer.getProperties().get(event.getSetting()));
            }
        });

        UtilServer.callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates(){
        gamerManager.getGamerRepository().processStatUpdates(true);
    }

}
