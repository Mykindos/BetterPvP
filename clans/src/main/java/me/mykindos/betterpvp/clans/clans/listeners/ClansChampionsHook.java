package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.clans.clans.core.EnergyItem;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@BPvPListener
@Singleton
@PluginAdapter("Champions")
public class ClansChampionsHook implements Listener {

    private final RoleManager roleManager;
    private final DamageLogManager damageLogManager;

    @Inject
    @Config(path = "clans.energy.energy-per-kill-min", defaultValue = "20")
    private int energyMinPerKill;

    @Inject
    @Config(path = "clans.energy.energy-per-kill-max", defaultValue = "30")
    private int energyMaxPerKill;

    @Inject
    private ClansChampionsHook(RoleManager roleManager, DamageLogManager damageLogManager) {
        this.roleManager = roleManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player killed = event.getPlayer();
        if (this.roleManager.getObject(killed.getUniqueId()).isEmpty()) {
            return;
        }

        final DamageLog lastDamage = this.damageLogManager.getLastDamager(killed);
        if (lastDamage == null || !(lastDamage.getDamager() instanceof Player)) {
            return;
        }

        int energy = UtilMath.RANDOM.ints(energyMinPerKill, energyMaxPerKill).findFirst().orElseThrow();
        event.getDrops().add(EnergyItem.SHARD.generateItem(energy, true));
    }

}
