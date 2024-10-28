package me.mykindos.betterpvp.core.client.gamer.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class GamerProtectionListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public GamerProtectionListener(EffectManager effectManager) {
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
