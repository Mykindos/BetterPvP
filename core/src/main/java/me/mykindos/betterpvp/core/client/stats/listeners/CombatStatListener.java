package me.mykindos.betterpvp.core.client.stats.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.ClientStat;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player)) return;
        final Client client = clientManager.search().online(player);
        final StatContainer container = client.getStatContainer();
        container.incrementStat(ClientStat.DEATHS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        final LivingEntity killed = event.getEntity();
        if (killed instanceof Player) return;
        final DamageLog lastDamager = damageLogManager.getLastDamager(event.getEntity());
        if (lastDamager == null) return;
        if (!(lastDamager.getDamager() instanceof Player player)) return;

        log.info("{} kill mob", player.getName()).submit();
        final Client client = clientManager.search().online(player);
        final StatContainer container = client.getStatContainer();
        container.incrementStat(ClientStat.MOB_KILLS, 1);
    }
}
