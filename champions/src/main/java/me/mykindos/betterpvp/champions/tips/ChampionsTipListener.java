package me.mykindos.betterpvp.champions.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.components.champions.Role;
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
public class ChampionsTipListener implements Listener {

    private final RoleManager roleManager;
    private final TipManager tipManager;

    @Inject
    ChampionsTipListener(RoleManager roleManager, TipManager tipManager) {
        this.roleManager = roleManager;
        this.tipManager = tipManager;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        WeighedList<Tip> tipList = event.getTipList();

        Optional<Role> roleOptional  = roleManager.getObject(player.getUniqueId());
        final Role role = roleOptional.orElse(null);

        tipManager.getTips().forEach(tip -> {
            if (tip instanceof ChampionsTip clanTip) {
                if (clanTip.isEnabled() && clanTip.isValid(player, role)) {
                    tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                }
            }
        });

    }

}
