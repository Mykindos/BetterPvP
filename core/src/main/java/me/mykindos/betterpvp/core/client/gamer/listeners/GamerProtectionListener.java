package me.mykindos.betterpvp.core.client.gamer.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Slf4j
@Singleton
@BPvPListener
public class GamerProtectionListener implements Listener {
    private final ClientManager clientManager;
    private final EffectManager effectManager;

    @Inject
    public GamerProtectionListener(ClientManager clientManager, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onLogin(ClientJoinEvent event) {
        long remainingProtection = event.getClient().getGamer().getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
        if (remainingProtection > 0) {
            effectManager.addEffect(event.getPlayer(), EffectTypes.PROTECTION, remainingProtection);
        }
        event.getClient().getGamer().setLastSafeNow();
    }

    @EventHandler
    public void onLogout(ClientQuitEvent event) {
        event.getClient().getGamer().updateRemainingProtection();
    }

}
