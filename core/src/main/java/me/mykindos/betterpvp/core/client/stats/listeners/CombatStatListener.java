package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class CombatStatListener implements Listener {
    private final ClientManager clientManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public CombatStatListener(ClientManager clientManager, DamageLogManager damageLogManager) {
        this.clientManager = clientManager;
        this.damageLogManager = damageLogManager;
    }
}
