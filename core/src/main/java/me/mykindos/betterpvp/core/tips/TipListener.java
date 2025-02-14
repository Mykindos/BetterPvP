package me.mykindos.betterpvp.core.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@CustomLog
@Singleton
@BPvPListener
public class TipListener implements Listener {

    @Inject
    @Config(path = "core.tips.timeBetweenTips", defaultValue = "150.0")
    public double timeBetweenTips;

    public final Core core;

    public final TipManager tipManager;

    public final ClientManager clientManager;

    @Inject
    public TipListener(Core core, ClientManager clientManager, TipManager tipManager) {
        super();
        this.core = core;
        this.clientManager = clientManager;
        this.tipManager = tipManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClientLogin(ClientJoinEvent event) {
        event.getClient().getGamer().setLastTipNow();
    }

    @UpdateEvent(delay = 10 * 1000, isAsync = true)
    public void tipSender() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            final Client client = clientManager.search().online(player);
            final Gamer gamer = client.getGamer();
            if ((boolean) client.getProperty(ClientProperty.TIPS_ENABLED).orElse(true)) {
                if (UtilTime.elapsed(gamer.getLastTip(), (long) timeBetweenTips * 1000L)) {
                    UtilServer.runTaskAsync(core, () -> UtilServer.callEvent(new TipEvent(player, gamer)));
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();

        WeighedList<Tip> tipList = event.getTipList();

        tipManager.getTips().forEach(tip -> {
            if (tip.isEnabled() && tip.isValid(player)) {
                tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
            }
        });

        if (tipList.size() > 0) {
            Tip tip = tipList.random();
            UtilMessage.message(player, "Tips", tip.getComponent());

            Bukkit.broadcastMessage(UtilMessage.infoAboutComponent(tip.getComponent()));
            log.info(UtilMessage.infoAboutComponent(tip.getComponent()));
            event.getGamer().setLastTipNow();
        } else {
            log.warn("No valid tips for " + player.getName()).submit();
        }

    }
}
