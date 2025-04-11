package me.mykindos.betterpvp.champions.champions.roles.listeners.passives;

        import com.google.inject.Inject;
        import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
        import me.mykindos.betterpvp.champions.champions.roles.RolePassive;
        import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
        import me.mykindos.betterpvp.core.components.champions.Role;
        import me.mykindos.betterpvp.core.config.Config;
        import me.mykindos.betterpvp.core.effects.EffectManager;
        import me.mykindos.betterpvp.core.effects.EffectTypes;
        import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
        import me.mykindos.betterpvp.core.listener.BPvPListener;
        import org.bukkit.Bukkit;
        import org.bukkit.entity.Player;
        import org.bukkit.event.EventHandler;
        import org.bukkit.event.Listener;
        import org.bukkit.event.entity.EntityDamageEvent;
        import org.bukkit.potion.PotionEffect;
        import org.bukkit.potion.PotionEffectType;

        import java.util.ArrayList;

@BPvPListener
public class AssassinPassiveListener implements Listener {

    @Inject
    @Config(path = "class.assassin.dealKnockback", defaultValue = "false")
    private boolean assassinDealKnockback;

    @Inject
    @Config(path = "class.assassin.receiveKnockback", defaultValue = "true")
    private boolean assassinReceiveKnockback;

    private final RoleManager roleManager;
    private final EffectManager effectManager;

    @Inject
    public AssassinPassiveListener(RoleManager roleManager, EffectManager effectManager) {
        this.roleManager = roleManager;
        this.effectManager = effectManager;

        ArrayList<RolePassive> sinPassives = RoleManager.rolePassiveDescs.getOrDefault(Role.ASSASSIN, new ArrayList<>());
        sinPassives.add(new RolePassive("Surgical Precision", "Melee attacks deal no knockback", true));
        sinPassives.add(new RolePassive("Blur", "You are granted permanent Speed 2", true));
        sinPassives.add(new RolePassive("Speedlock", "You receive no knockback while Slowed", false));

        // Need this line since we probably had to create a new ArrayList
        RoleManager.rolePassiveDescs.put(Role.ASSASSIN, sinPassives);
    }

    /**
     * Surgical Precision Passive & Speedlock Passive
     */
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
            if (!assassinReceiveKnockback || effectManager.hasEffect(damagee, EffectTypes.SLOWNESS)) {
                if (roleManager.hasRole(damagee, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

    }

    /**
     * Blur Passive
     */
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
