package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

@Singleton
@BPvPListener
public class Backstab extends Skill implements PassiveSkill, Listener {


    @Inject
    public Backstab(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Backstab";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hitting an enemy from behind will",
                "increase your damage by <val>" + (2 + level) + "0%"};
    }

    @EventHandler
    public void onEntDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player daamger)) return;
        if (!UtilPlayer.isHoldingItem(daamger, SkillWeapons.SWORDS)) return;
        int level = getLevel(daamger);
        if (level <= 0) return;


        if (UtilMath.getAngle(daamger.getLocation().getDirection(), event.getDamagee().getLocation().getDirection()) < 60) {


            event.setDamage(event.getDamage() * (1.2 + (level * 0.1)));
            daamger.getWorld().playEffect(event.getDamagee().getLocation().add(0, 1, 0), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
            }

            event.setReason("Backstab");
        }
    @Override
    public String getDefaultClassString() {
        return "assassin";
    }
    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }


}
