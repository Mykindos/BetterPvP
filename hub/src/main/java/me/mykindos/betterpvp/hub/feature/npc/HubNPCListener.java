package me.mykindos.betterpvp.hub.feature.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.npc.NPCRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.hub.Hub;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
public class HubNPCListener implements Listener {

    @Inject
    private NPCRegistry registry;

    @Inject
    private HubNPCFactory factory;

    @Inject
    private Hub hub;

    private boolean loaded;

    @EventHandler
    public void onRegister(ModelRegistrationEvent event) {
        if (loaded || event.getPhase() != ModelGenerator.Phase.FINISHED) {
            return;
        }

        UtilServer.runTask(hub, () -> {
            loaded = factory.tryLoad(hub);
        });
    }
}
