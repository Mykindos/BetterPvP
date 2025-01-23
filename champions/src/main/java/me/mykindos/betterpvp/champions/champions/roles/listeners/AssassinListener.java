package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareArrowSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.reflections.Reflections;

import java.util.List;

@BPvPListener
public class AssassinListener implements Listener {

    @Inject
    @Config(path = "class.assassin.dealKnockback", defaultValue = "false")
    private boolean assassinDealKnockback;

    @Inject
    @Config(path = "class.assassin.receiveKnockback", defaultValue = "true")
    private boolean assassinReceiveKnockback;

    @Inject
    @Config(path = "class.assassin.receiveSlownessKnockback", defaultValue = "true")
    private boolean assassinSlownessKnockback;

    @Inject
    @Config(path = "class.assassin.bow.overrideArrowDamage", defaultValue = "true")
    private boolean overrideArrowDamage;

    @Inject
    @Config(path = "class.assassin.bow.arrowDamage", defaultValue = "0.0")
    private double arrowDamage;

    @Inject
    @Config(path = "class.assassin.bow.onlyWhilePrepared", defaultValue = "false")
    private boolean bowOnlyWhilePrepared;

    private final RoleManager roleManager;
    private final EffectManager effectManager;
    private final List<PrepareArrowSkill> prepareSkills;

    @Inject
    public AssassinListener(Champions champions, RoleManager roleManager, EffectManager effectManager) {
        this.roleManager = roleManager;
        this.effectManager = effectManager;
        final Reflections reflections = new Reflections(Champions.class.getPackageName());
        this.prepareSkills = reflections.getSubTypesOf(PrepareArrowSkill.class)
                .stream()
                .map(type -> (PrepareArrowSkill) champions.getInjector().getInstance(type))
                .filter(skill -> Role.ASSASSIN.equals(skill.getClassType()))
                .toList();
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
            if (!assassinReceiveKnockback || (!assassinSlownessKnockback && effectManager.hasEffect(damagee, EffectTypes.SLOWNESS))) {
                if (roleManager.hasRole(damagee, Role.ASSASSIN)) {
                    event.setKnockback(false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.getShooter() instanceof Player player)) {
            return;
        }

        if (roleManager.getObject(player.getUniqueId()).orElse(null) != Role.ASSASSIN) {
            return;
        }

        if (bowOnlyWhilePrepared && prepareSkills.stream().noneMatch(skill -> skill.getActive().contains(player.getUniqueId()))) {
            event.setCancelled(true);
            UtilMessage.simpleMessage(player, "Bow", "You can only use your bow with a prepared arrow.");
        }

        if (overrideArrowDamage) {
            arrow.setDamage(arrowDamage);
        }
    }

    @UpdateEvent(delay = 500)
    public void checkRoleBuffs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            roleManager.getObject(player.getUniqueId()).ifPresent(role -> {
                if (role == Role.ASSASSIN) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));
                }
            });
        }
    }
}
