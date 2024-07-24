package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
    public void onAssassinKnockback(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        if (event.getDamager() instanceof Player damager) {
            if (!assassinDealKnockback) {
                if (roleManager.hasRole(damager, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

        if (event.getDamagee() instanceof Player damagee) {
            if (!assassinReceiveKnockback) {
                if (roleManager.hasRole(damagee, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

    }

    @UpdateEvent(delay = 500)
    public void checkRoleBuffs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            roleManager.getObject(player.getUniqueId()).ifPresent(role -> {
                if (role == Role.ASSASSIN) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));
                }
            });
        }
    }
}
