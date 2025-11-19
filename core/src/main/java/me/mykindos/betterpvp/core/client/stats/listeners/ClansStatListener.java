package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.components.clans.events.ClanAddExperienceEvent;
import me.mykindos.betterpvp.core.components.clans.events.ClansDropEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ClansStatListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public ClansStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGainClanExperience(ClanAddExperienceEvent event) {
        final StatContainer container = clientManager.search().online(event.getPlayer()).getStatContainer();
        container.incrementStat(ClientStat.CLANS_CLANS_EXPERIENCE, (long) (event.getExperience() * 1000L));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIncrementEnergy(ClansDropEnergyEvent event) {
        if (event.getLivingEntity() instanceof Player player) {
            final StatContainer container = clientManager.search().online(player).getStatContainer();
            container.incrementStat(ClientStat.CLANS_ENERGY_DROPPED, event.getAmount());
        }
    }
}
