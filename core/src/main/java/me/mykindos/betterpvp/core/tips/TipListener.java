package me.mykindos.betterpvp.core.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
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
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@BPvPListener
public class TipListener implements Listener {

    @Inject
    @Config(path = "core.tips.timeBetweenTips", defaultValue = "150")
    public int timeBetweenTips;

    public final Core core;

    public final TipManager tipManager;

    public final GamerManager gamerManager;
    @Inject
    public TipListener(Core core, GamerManager gamerManager, TipManager tipManager) {
        super();
        this.core = core;
        this.gamerManager = gamerManager;
        this.tipManager = tipManager;

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClientLogin(ClientLoginEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        Gamer gamer;
        if (gamerOptional.isPresent()) {
            gamer = gamerOptional.get();
            gamer.setLastTipNow();
        }
    }

    @UpdateEvent(delay = 10 * 1000, isAsync = true)
    public void tipSender() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            UtilServer.runTaskLaterAsync(core, () -> UtilServer.callEvent(new TipEvent(player)), 5);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
        if (gamerOptional.isEmpty()) {
            event.cancel("Gamer not found.");
            return;
        }
        Gamer gamer = gamerOptional.get();

        WeighedList<Tip> tipList = event.getTipList();

        if ((boolean) gamer.getProperty(GamerProperty.TIPS_ENABLED).orElse(true) &&
                UtilTime.elapsed(gamer.getLastTip(), (long) 1 * 1000)
                ) {

            tipManager.getTips().forEach(tip -> {
                if (tip.isValid(player)) {
                    tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                }
            });

            if (tipList.size() > 0) {
                Tip tip = tipList.random();
                UtilMessage.message(player, "Tips", tip.getComponent());
                gamer.setLastTipNow();
            }
        }
    }
}
