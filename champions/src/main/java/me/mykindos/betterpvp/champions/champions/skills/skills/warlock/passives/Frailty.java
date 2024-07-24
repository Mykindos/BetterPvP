package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
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

    private double baseHealthPercent;

    private double healthPercentIncreasePerLevel;

    private double baseDamagePercent;

    private double damagePercentIncreasePerLevel;

    @Inject
    public Frailty(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Frailty";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Nearby enemies that fall below " + getValueString(this::getHealthPercent, level, 100, "%", 0) + " health",
                "take " + getValueString(this::getDamagePercent, level, 100, "%", 0) + " more damage from your melee attacks"
        };
    }

    public double getHealthPercent(int level) {
        return baseHealthPercent + ((level - 1) * healthPercentIncreasePerLevel);
    }

    public double getDamagePercent(int level) {
        return baseDamagePercent + ((level - 1) * damagePercentIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @EventHandler
    public void onEquip(SkillEquipEvent event) {
        if (event.getSkill().equals(this)) {
            active.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill().equals(this)) {
            active.remove(event.getPlayer().getUniqueId());
        }
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
                int level = getLevel(player);
                if (level <= 0) return;
                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, player.getLocation(), 5)) {
                    if (UtilPlayer.getHealthPercentage(target) < getHealthPercent(level)) {
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

        int level = getLevel(damager);
        if (level > 0) {
            if (event.getDamagee() instanceof Player damagee) {
                if (championsManager.getEffects().hasEffect(damagee, EffectTypes.IMMUNE)) {
                    return;
                }
            }

            if (UtilPlayer.getHealthPercentage(event.getDamagee()) < getHealthPercent(level)) {
                event.setDamage(event.getDamage() * (1 + getDamagePercent(level)));
            }
        }

    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public void loadSkillConfig() {
        baseHealthPercent = getConfig("baseHealthPercent", 0.30, Double.class);
        healthPercentIncreasePerLevel = getConfig("healthPercentIncreasePerLevel", 0.10, Double.class);

        baseDamagePercent = getConfig("baseDamagePercent", 0.15, Double.class);
        damagePercentIncreasePerLevel = getConfig("damagePercentIncreasePerLevel", 0.05, Double.class);
    }

}
