package me.mykindos.betterpvp.clans.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Cleave extends Skill implements PassiveSkill, Listener {

    private double baseDistance;
    private double baseDamage;

    @Inject
    public Cleave(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Cleave";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your axe attacks cleave onto nearby targets and deal damage.",
                "",
                "Damage: " + ChatColor.GREEN + (baseDamage + level) + ChatColor.GRAY,
                "Distance: " + ChatColor.GREEN + (baseDistance + level) + ChatColor.GRAY,
                "",
                "Only applies to axes."
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onCustomDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!UtilPlayer.isHoldingItem(damager, SkillWeapons.AXES)) return;

        int level = getLevel(damager);
        if (level > 0) {
            for (LivingEntity target : UtilEntity.getNearbyEntities(damager, damager.getLocation(), baseDistance + level)) {
                if (target.equals(damager)) continue;
                if (target.equals(event.getDamagee())) continue;

                UtilDamage.doCustomDamage(new CustomDamageEvent(target, damager, null, DamageCause.ENTITY_ATTACK, (baseDamage + level), true, "Cleave"));
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDistance = getConfig("baseDistance", 2.0, Double.class);
        baseDamage = getConfig("baseDamage", 4.0, Double.class);
    }


}
