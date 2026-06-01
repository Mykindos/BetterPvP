package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class GamerStatListener implements Listener {

    private final ClientManager clientManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public GamerStatListener(ClientManager clientManager, DamageLogManager damageLogManager) {
        this.clientManager = clientManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler
    public void onSettingsUpdated(GamerPropertyUpdateEvent event) {
        clientManager.saveGamerProperty(event.getContainer(), event.getProperty(), event.getNewValue());
    }

}
