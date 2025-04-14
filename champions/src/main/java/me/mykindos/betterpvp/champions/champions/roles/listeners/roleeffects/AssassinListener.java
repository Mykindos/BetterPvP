package me.mykindos.betterpvp.champions.champions.roles.listeners.roleeffects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.RoleEffect;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@BPvPListener
@Singleton
public class AssassinListener implements Listener, ConfigAccessor {

    // <editor-fold defaultstate="collapsed" desc="Config">
    private boolean surgicalPrecisionIsEnabled;
    private String surgicalPrecisionName;
    private boolean surgicalPrecisionIsBuff;

    private boolean blurIsEnabled;
    private String blurName;
    private boolean blurIsBuff;

    private boolean speedlockIsEnabled;
    private String speedlockName;
    private boolean speedlockIsBuff;
    // </editor-fold>

    private final RoleManager roleManager;
    private final EffectManager effectManager;

    @Inject
    public AssassinListener(Champions champions, RoleManager roleManager, EffectManager effectManager) {
        this.roleManager = roleManager;
        this.effectManager = effectManager;
        loadConfig(champions.getConfig());

        ArrayList<RoleEffect> sinPassives = RoleManager.rolePassiveDescs.getOrDefault(Role.ASSASSIN, new ArrayList<>());
        if (surgicalPrecisionIsEnabled) {
            TextComponent surgicalPrecisionDescription = Component.text("Your melee attacks deal no knockback");
            sinPassives.add(new RoleEffect(surgicalPrecisionName, surgicalPrecisionDescription, surgicalPrecisionIsBuff));
        }

        if (blurIsEnabled) {
            TextComponent blurDescription = Component.text("You are granted permanent ")
                    .append(Component.text("Speed 2").color(NamedTextColor.WHITE));
            sinPassives.add(new RoleEffect(blurName, blurDescription, blurIsBuff));
        }

        if (speedlockIsEnabled) {
            TextComponent speedlockDescription = Component.text("You receive no knockback while ")
                    .append(Component.text("Slowed").color(NamedTextColor.WHITE));
            sinPassives.add(new RoleEffect(speedlockName, speedlockDescription, speedlockIsBuff));
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
            if (surgicalPrecisionIsEnabled) {
                if (roleManager.hasRole(damager, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }

        if (event.getDamagee() instanceof Player damagee) {
            if (!speedlockIsEnabled || effectManager.hasEffect(damagee, EffectTypes.SLOWNESS)) {
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
        if (!blurIsEnabled) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            roleManager.getObject(player.getUniqueId()).ifPresent(role -> {
                if (role == Role.ASSASSIN) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));
                }
            });
        }
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.surgicalPrecisionIsEnabled = config.getOrSaveBoolean("class.assassin.surgicalPrecision.enabled", true);
        this.surgicalPrecisionName = config.getOrSaveString("class.assassin.surgicalPrecision.name", "Surgical Precision");
        this.surgicalPrecisionIsBuff = config.getOrSaveBoolean("class.assassin.surgicalPrecision.isBuff", true);

        this.blurIsEnabled = config.getOrSaveBoolean("class.assassin.blur.enabled", true);
        this.blurName = config.getOrSaveString("class.assassin.blur.name", "Blur");
        this.blurIsBuff = config.getOrSaveBoolean("class.assassin.blur.isBuff", true);

        this.speedlockIsEnabled = config.getOrSaveBoolean("class.assassin.speedlock.enabled", true);
        this.speedlockName = config.getOrSaveString("class.assassin.speedlock.name", "Speedlock");
        this.speedlockIsBuff = config.getOrSaveBoolean("class.assassin.speedlock.isBuff", false);
    }
}

