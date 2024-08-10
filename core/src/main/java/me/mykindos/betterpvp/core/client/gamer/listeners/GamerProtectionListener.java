package me.mykindos.betterpvp.core.client.gamer.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
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
    public void onProtectionExpire(EffectExpireEvent event) {
        if (event.getTarget() instanceof Player player && event.getEffect().getEffectType() == EffectTypes.PROTECTION) {
            Gamer gamer = clientManager.search().online(player).getGamer();
            long remainingProtection = gamer.getLongProperty(GamerProperty.REMAINING_PVP_PROTECTION);
            if (remainingProtection > 0) {
                effectManager.addEffect(player, EffectTypes.PROTECTION, remainingProtection);
            }
        }
    }

}
