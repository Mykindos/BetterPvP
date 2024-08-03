package me.mykindos.betterpvp.core.coretips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.core.tips.TipEvent;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class CoreTipListener implements Listener {

    private final TipManager tipManager;

    @Inject
    CoreTipListener(TipManager tipManager) {
        this.tipManager = tipManager;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        WeighedList<Tip> tipList = event.getTipList();

        tipManager.getTips().forEach(tip -> {
            if (tip instanceof CoreTip coreTip) {
                if (coreTip.isEnabled() && coreTip.isValid(player)) {
                    tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                }
            }
        });

    }

}
