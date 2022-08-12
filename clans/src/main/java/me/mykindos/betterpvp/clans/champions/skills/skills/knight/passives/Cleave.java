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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Cleave extends Skill implements PassiveSkill, Listener {

    private double baseDistance;

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
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!UtilPlayer.isHoldingItem(damager, SkillWeapons.AXES)) return;
        if (event.getReason().equals(getName())) return; // Don't get stuck in an endless damage loop

        int level = getLevel(damager);
        if (level > 0) {
            for (var target : UtilEntity.getNearbyEntities(damager, damager.getLocation(), baseDistance + level, EntityProperty.ENEMY)) {
                if (target.get().equals(event.getDamagee())) continue;

                UtilDamage.doCustomDamage(new CustomDamageEvent(target.getKey(), damager, null, DamageCause.ENTITY_ATTACK, event.getDamage(), true, getName()));
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDistance = getConfig("baseDistance", 2.0, Double.class);
    }


}
