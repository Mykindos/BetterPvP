package me.mykindos.betterpvp.champions.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@Singleton
@BPvPListener
public class Intimidation extends Skill implements PassiveSkill {

    private double radius;

    @Inject
    public Intimidation(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Intimidation";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Give every enemy within <val" + (radius + level) + "<val> blocks <effect>Slowness 1",
                "Hold shift to reduce the radius to <val>"+ ((radius + level)/2) + "</val> blocks",
                "and give enemies <effect>Slowness II</effect> instead"};
    }

    @UpdateEvent(delay = 250)
    public void intimidate(Player player) {
        int level = getLevel(player);
        if(level>0) {
            double currentRadius;
            PotionEffectType slownessType = PotionEffectType.SLOW;
            int slownessLevel;

            if(player.isSneaking()) {
                currentRadius = (radius + level) / 2;
                slownessLevel = 1;
            } else {
                currentRadius = radius + level;
                slownessLevel = 0;
            }

            List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), currentRadius);

            for(Player enemy : nearbyEnemies) {
                enemy.addPotionEffect(new PotionEffect(slownessType, 1, slownessLevel, false, true));
            }
        }
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig(){
        radius = getConfig("radius", 7.0, Double.class);
    }
}