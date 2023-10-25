package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class Frailty extends Skill implements PassiveSkill {

    private final Set<UUID> active = new HashSet<>();

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
                "Nearby enemies that fall below <val>" + (40 + ((level - 1) * 10)) + "%" + "</val> health",
                "take <val>" + (20 + ((level - 1) * 5)) + "%" + "</val> more damage from your melee attacks"
        };
    }

    @Override
    public Set<Role> getClassTypes() {
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
                for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), 5)) {
                    if (target.getHealth() / UtilPlayer.getMaxHealth(target) * 100 < (40 + ((level - 1) * 10))) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 30, 0));
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
                if (championsManager.getEffects().hasEffect(damagee, EffectType.IMMUNETOEFFECTS)) {
                    return;
                }
            }

            if (event.getDamagee().getHealth() / UtilPlayer.getMaxHealth(event.getDamagee()) * 100 < (40 + (level - 1) * 10)) {
                event.setDamage(event.getDamage() * 1.20 + ((level - 1) * 0.05));
            }
        }

    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }


}
