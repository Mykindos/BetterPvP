package me.mykindos.betterpvp.champions.champions.roles.listeners;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.inject.Inject;

@BPvPListener
public class AssassinListener implements Listener {

    @Inject
    @Config(path = "class.assassin.dealKnockback", defaultValue = "false")
    private boolean assassinDealKnockback;

    @Inject
    @Config(path = "class.assassin.receiveKnockback", defaultValue = "true")
    private boolean assassinReceiveKnockback;

    private final RoleManager roleManager;

    @Inject
    public AssassinListener(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @EventHandler
    public void onFallDamage(CustomDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;

        if (roleManager.hasRole(damagee, Role.ASSASSIN)) {
            event.cancel("Feather falling");
        }

    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onAssassinKnockback(CustomDamageEvent event) {

        if (event.getDamager() instanceof Player damager) {
            if (!assassinDealKnockback) {
                if (roleManager.hasRole(damager, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

        if(event.getDamagee() instanceof Player damagee){
            if(!assassinReceiveKnockback) {
                if(roleManager.hasRole(damagee, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

    }
}
