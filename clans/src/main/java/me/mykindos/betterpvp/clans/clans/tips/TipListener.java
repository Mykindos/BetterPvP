package me.mykindos.betterpvp.clans.clans.tips;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.listeners.ClanListener;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;

@BPvPListener
public class TipListener extends ClanListener {

    @Inject
    @Config(path = "clans.tips.timeBetweenTips", defaultValue = "300")
    private int timeBetweenTips;

    private final Clans clans;
    @Inject
    public TipListener(Clans clans, ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
        this.clans = clans;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClientLogin(ClientLoginEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        Gamer gamer;
        if (gamerOptional.isPresent()) {
            gamer = gamerOptional.get();
            gamer.setLastTip(System.currentTimeMillis());
        }
    }

    @UpdateEvent(delay = 10 * 1000, isAsync = true)
    public void tipSender() {
        gamerManager.getObjectsValues().forEach(gamer -> {
            if (gamer.getPlayer() != null){
                UtilServer.runTaskLaterAsync(clans, () -> UtilServer.callEvent(new TipEvent(gamer)), 5);
            }

        });

    }

    @EventHandler
    public void onTip(TipEvent event) {
        Gamer gamer = event.getGamer();
        if ((boolean) gamer.getProperty(GamerProperty.TIPS_ENABLED).orElse(true) && UtilTime.elapsed(gamer.getLastTip(), 30 * 1000) && gamer.getPlayer() != null) {
            UtilMessage.message(gamer.getPlayer(), "Clans", Component.text("You can create a CLan by running /c create <name>"));
        }

    }

}
