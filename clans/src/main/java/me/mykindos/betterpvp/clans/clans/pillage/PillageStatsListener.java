package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.concurrent.CompletableFuture;

@Singleton
@BPvPListener
public class PillageStatsListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    public PillageStatsListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPillageStart(PillageStartEvent event) {
        CompletableFuture.supplyAsync(() -> {
            event.getPillage().getPillaged().getMembers().stream()
                    .map(clanMember -> clientManager.search().offline(clanMember.getUuid()))
                    .forEach(future -> future.thenAccept(
                            clientOptional ->
                                    clientOptional.ifPresent(client -> client.getStatContainer().incrementStat(ClientStat.CLANS_DEFEND_PILLAGE, 1))
                    ));

            event.getPillage().getPillager().getMembers().stream()
                    .map(clanMember -> clientManager.search().offline(clanMember.getUuid()))
                    .forEach(future -> future.thenAccept(
                            clientOptional ->
                                    clientOptional.ifPresent(client -> client.getStatContainer().incrementStat(ClientStat.CLANS_ATTACK_PILLAGE, 1))
                    ));
            return null;
        });

    }
}
