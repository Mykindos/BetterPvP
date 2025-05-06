package me.mykindos.betterpvp.champions.champions.roles.listeners.roleeffects;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleEffect;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

@BPvPListener
@Singleton
public class AssassinListener implements Listener, ConfigAccessor {

    // <editor-fold defaultstate="collapsed" desc="Config">
    private boolean meleeDealsNoKnockbackIsEnabled;
    private boolean meleeDealsNoKnockbackIsBuff;

    private boolean speedBuffIsEnabled;
    private boolean speedBuffIsBuff;

    private boolean noKnockbackReceivedWhenSlowedIsEnabled;
    private boolean noKnockbackReceivedWhenSlowedIsBuff;
    // </editor-fold>

    private final RoleManager roleManager;
    private final EffectManager effectManager;

    @Inject
    public AssassinListener(Champions champions, RoleManager roleManager, EffectManager effectManager) {
        this.roleManager = roleManager;
        this.effectManager = effectManager;
        loadConfig(champions.getConfig());

        ArrayList<RoleEffect> sinPassives = RoleManager.rolePassiveDescs.getOrDefault(Role.ASSASSIN, new ArrayList<>());
        if (meleeDealsNoKnockbackIsEnabled) {
            TextComponent meleeDealsNoKnockbackDescription = Component.text("Melee attacks deal no knockback");
            sinPassives.add(new RoleEffect(meleeDealsNoKnockbackDescription, meleeDealsNoKnockbackIsBuff));
        }

        if (speedBuffIsEnabled) {
            TextComponent speedBuffDescription = Component.text("Permanently granted ")
                    .append(Component.text("Speed 2").color(NamedTextColor.WHITE));
            sinPassives.add(new RoleEffect(speedBuffDescription, speedBuffIsBuff));
        }

        if (noKnockbackReceivedWhenSlowedIsEnabled) {
            TextComponent noKnockbackReceivedWhenSlowedDescription = Component.text("Cannot be knocked back while ")
                    .append(Component.text("Slowed").color(NamedTextColor.WHITE));
            sinPassives.add(new RoleEffect(noKnockbackReceivedWhenSlowedDescription, noKnockbackReceivedWhenSlowedIsBuff));
        }

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
            if (meleeDealsNoKnockbackIsEnabled) {
                if (roleManager.hasRole(damager, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

        if (event.getDamagee() instanceof Player damagee) {
            if (!noKnockbackReceivedWhenSlowedIsEnabled || effectManager.hasEffect(damagee, EffectTypes.SLOWNESS)) {
                if (roleManager.hasRole(damagee, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAssassinArrowDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow)) return;
        if (event.getProjectile().getShooter() instanceof Player player) {
            if (roleManager.hasRole(player, Role.ASSASSIN)) {
                event.setDamage(0);
            }
        }
    }

    /**
     * Blur Passive
     */
    @UpdateEvent(delay = 500)
    public void checkRoleBuffs() {
        if (!speedBuffIsEnabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Role role = roleManager.getObject(player.getUniqueId()).orElse(null);
            if (role == Role.ASSASSIN) {
                effectManager.addEffect(player, null, EffectTypes.SPEED, "Assassin", 2, -1, true, true);
            } else {
                effectManager.removeEffect(player, EffectTypes.SPEED, "Assassin", false);
            }
        }
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.meleeDealsNoKnockbackIsEnabled = config.getOrSaveBoolean("class.assassin.melee-deals-no-knockback.enabled", true);
        this.meleeDealsNoKnockbackIsBuff = config.getOrSaveBoolean("class.assassin.melee-deals-no-knockback.isBuff", true);

        this.speedBuffIsEnabled = config.getOrSaveBoolean("class.assassin.speed-buff.enabled", true);
        this.speedBuffIsBuff = config.getOrSaveBoolean("class.assassin.speed-buff.isBuff", true);

        this.noKnockbackReceivedWhenSlowedIsEnabled = config.getOrSaveBoolean("class.assassin.no-knockback-received-when-slowed.enabled", true);
        this.noKnockbackReceivedWhenSlowedIsBuff = config.getOrSaveBoolean("class.assassin.no-knockback-received-when-slowed.isBuff", false);
    }
}

