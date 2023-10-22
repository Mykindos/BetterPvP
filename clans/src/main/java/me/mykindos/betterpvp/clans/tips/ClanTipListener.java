package me.mykindos.betterpvp.clans.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.core.tips.TipEvent;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@BPvPListener
public class ClanTipListener implements Listener {

    public final ClanManager clanManager;

    private final TipManager tipManager;

    @Inject
    ClanTipListener(ClanManager clanManager, TipManager tipManager) {
        this.clanManager = clanManager;
        this.tipManager = tipManager;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        WeighedList<Tip> tipList = event.getTipList();

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        final Clan clan = clanOptional.orElse(null);

        tipManager.getTips().forEach(tip -> {
            if (tip instanceof ClanTip clanTip) {
                if (clanTip.isEnabled() && clanTip.isValid(player, clan)) {
                    tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                }
            }
        });

    }

}
