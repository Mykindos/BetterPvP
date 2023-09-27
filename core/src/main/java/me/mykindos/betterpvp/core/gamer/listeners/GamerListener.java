package me.mykindos.betterpvp.core.gamer.listeners;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
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

    private final Core core;
    private final GamerManager gamerManager;

    @Inject
    public GamerListener(Core core, GamerManager gamerManager){
        this.core = core;
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onTick(ServerTickStartEvent event) {
        gamerManager.getObjects().values().forEach(gamer -> gamer.getActionBar().show(gamer));
        gamerManager.getObjects().values().forEach(gamer -> gamer.getTitleQueue().show(gamer));
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

        Bukkit.getOnlinePlayers().forEach(player ->
                UtilServer.runTaskLater(core, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)), 1));

    }

    private void checkUnsetProperties(Gamer gamer) {
        Optional<Integer> coinsOptional = gamer.getProperty(GamerProperty.BALANCE);
        if(coinsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.BALANCE, defaultCoins);
        }

        Optional<Integer> fragmentsOptional = gamer.getProperty(GamerProperty.FRAGMENTS);
        if(fragmentsOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.FRAGMENTS, defaultFragments);
        }

        Optional<Boolean> sidebarOptional = gamer.getProperty(GamerProperty.SIDEBAR_ENABLED);
        if(sidebarOptional.isEmpty()){
            gamer.saveProperty(GamerProperty.SIDEBAR_ENABLED, true);
        }
    }

}
