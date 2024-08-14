package me.mykindos.betterpvp.progression.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.core.tips.TipEvent;
import me.mykindos.betterpvp.core.tips.TipManager;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@BPvPListener
public class ProgressionTipListener implements Listener {

    private final ProfessionProfileManager professionProfileManager;
    private final TipManager tipManager;

    @Inject
    ProgressionTipListener(ProfessionProfileManager professionProfileManager, TipManager tipManager) {
        this.professionProfileManager = professionProfileManager;
        this.tipManager = tipManager;
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onTip(TipEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        WeighedList<Tip> tipList = event.getTipList();

        Optional<ProfessionProfile> professionProfileOptional  = professionProfileManager.getObject(player.getUniqueId());
        final ProfessionProfile professionProfile = professionProfileOptional.orElse(null);

        tipManager.getTips().forEach(tip -> {
            if (tip instanceof ProgressionTip progressionTip) {
                if (progressionTip.isEnabled() && progressionTip.isValid(player, professionProfile)) {
                    tipList.add(tip.getCategoryWeight(), tip.getWeight(), tip);
                }
            }
        });

    }

}
