package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Cleave extends Skill implements PassiveSkill, Listener {

    private double baseDistance;

    private double distanceIncreasePerLevel;

    @Inject
    public Cleave(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleave";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your axe attacks cleave onto nearby targets and deal damage",
                "",
                "Distance: <val>" + getDistance(level),
        };
    }

    public double getDistance(int level) {
        return baseDistance + level * distanceIncreasePerLevel;
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
        if (event.hasReason(getName())) return; // Don't get stuck in an endless damage loop

        int level = getLevel(damager);
        if (level > 0) {
            for (var target : UtilEntity.getNearbyEntities(damager, damager.getLocation(), getDistance(level), EntityProperty.ENEMY)) {
                if (target.get().equals(event.getDamagee())) continue;

                UtilDamage.doCustomDamage(new CustomDamageEvent(target.getKey(), damager, null, DamageCause.ENTITY_ATTACK, event.getDamage(), true, getName()));
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDistance = getConfig("baseDistance", 2.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 1.0, Double.class);
    }


}
