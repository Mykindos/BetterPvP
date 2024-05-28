package me.mykindos.betterpvp.progression.profile.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.event.ProfessionPropertyUpdateEvent;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@Singleton
@BPvPListener
public class ProfessionProfileListener implements Listener {

    private final Progression progression;
    private final ProfessionProfileManager professionProfileManager;

    @Inject
    public ProfessionProfileListener(Progression progression, ProfessionProfileManager professionProfileManager) {
        this.progression = progression;
        this.professionProfileManager = professionProfileManager;
    }

    @EventHandler
    public void onClientJoin(ClientJoinEvent event) {
        UtilServer.runTaskAsync(progression, () -> {
            professionProfileManager.loadProfile(event.getPlayer().getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        professionProfileManager.removeObject(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPropertyUpdate(ProfessionPropertyUpdateEvent event) {
        professionProfileManager.getRepository().saveProperty(event.getUuid(), event.getProfession(), event.getProperty(), event.getValue());
    }

    @UpdateEvent(delay = 60 * 5 * 1000, isAsync = true)
    public void cycleSave() {
        professionProfileManager.getRepository().processStatUpdates(true);
    }


}
