package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class Frailty extends Skill implements PassiveSkill, OffensiveSkill {

    private final Set<UUID> active = new HashSet<>();

    @Getter
    private double healthPercent;
    @Getter
    private double damagePercent;


    @Inject
    public Frailty(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Frailty";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Nearby enemies that fall below <val>" + UtilFormat.formatNumber(getHealthPercent() * 100, 0) + "</val> health",
                "take <val>" + UtilFormat.formatNumber(getDamagePercent() * 100, 0) + "</val> more damage from your melee attacks"
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        active.add(player.getUniqueId());
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        active.remove(player.getUniqueId());
    }

    @UpdateEvent(delay = 1000)
    public void monitorActives() {
        active.removeIf(uuid -> {

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return !hasSkill(player);
            }

            return true;
        });

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (hasSkill(player)) {
                active.add(player.getUniqueId());
            }
        });

    }

    @UpdateEvent(delay = 1000)
    public void onUpdate() {
        if (active.isEmpty()) return;
        active.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (!hasSkill(player)) return;
                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), 5)) {
                    if (UtilPlayer.getHealthPercentage(target) < getHealthPercent()) {
                        championsManager.getEffects().addEffect(target, player, EffectTypes.WITHER, 1, 1500);
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (hasSkill(damager)) {
            if (event.getDamagee() instanceof Player damagee) {
                if (championsManager.getEffects().hasEffect(damagee, EffectTypes.IMMUNE)) {
                    return;
                }
            }

            if (UtilPlayer.getHealthPercentage(event.getDamagee()) < getHealthPercent()) {
                Location locationToPlayEffect = event.getDamagee().getLocation().add(0, 1, 0);
                event.getDamagee().getWorld().playEffect(locationToPlayEffect, Effect.COPPER_WAX_ON, 0);
                double damageIncrease = 1 + getDamagePercent();
                event.setDamage(event.getDamage() * damageIncrease);
            }
        }

    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public void loadSkillConfig() {
        healthPercent = getConfig("healthPercent", 0.30, Double.class);

        damagePercent = getConfig("damagePercent", 0.15, Double.class);
    }

}
