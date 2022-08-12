package me.mykindos.betterpvp.clans.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillEquipEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    public Frailty(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Frailty";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Nearby enemies that fall below " + ChatColor.GREEN + (40 + ((level - 1) * 10)) + "%" + ChatColor.GRAY + " health",
                "take " + ChatColor.GREEN + (20 + ((level - 1) * 5)) + "%" + ChatColor.GRAY + " more damage from only you."
        };
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
                for (var data : UtilPlayer.getNearbyPlayers(player, player.getLocation(), 5, EntityProperty.ENEMY)) {
                    Player target = data.get();
                    if (target.getHealth() / UtilPlayer.getMaxHealth(target) * 100 < (40 + ((getLevel(player) - 1) * 10))) {
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
