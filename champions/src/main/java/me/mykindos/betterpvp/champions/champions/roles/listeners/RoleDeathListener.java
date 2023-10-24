package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@BPvPListener
public class RoleDeathListener implements Listener {

    private final RoleManager roleManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public RoleDeathListener(RoleManager roleManager, DamageLogManager damageLogManager) {
        this.roleManager = roleManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killed = event.getPlayer();
        DamageLog lastDamaged = damageLogManager.getLastDamager(killed);
        if (lastDamaged == null) return;
        if (!(lastDamaged.getDamager() instanceof Player killer)) return;

        Role killedRole = roleManager.getObject(killed.getUniqueId()).orElse(null);
        Role killerRole = roleManager.getObject(killer.getUniqueId()).orElse(null);
        roleManager.getRepository().saveKillDeathData(killedRole, killerRole);
    }
}
