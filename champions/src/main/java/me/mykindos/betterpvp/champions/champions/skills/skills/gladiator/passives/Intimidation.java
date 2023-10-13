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
import org.bukkit.util.Vector;

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
                "Every enemy facing away from you within <val" + (radius + (level-1)) + "<val> blocks will get <effect>Slowness 1",
        };
    }

    private boolean isFacingAway(Player source, Player target) {
        Vector toTarget = target.getLocation().subtract(source.getLocation()).toVector().normalize();
        Vector direction = source.getLocation().getDirection();

        return toTarget.dot(direction) < 0;
    }

    public void intimidate(Player player) {
        int level = getLevel(player);
        if(level>0) {
            double currentRadius;
            PotionEffectType slownessType = PotionEffectType.SLOW;
            currentRadius = radius + (level-1);

            List<Player> nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), currentRadius);

            for(Player enemy : nearbyEnemies) {
                if(isFacingAway(enemy, player)) {
                    enemy.addPotionEffect(new PotionEffect(slownessType, 1, 0, false, true));
                }
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
        radius = getConfig("radius", 3, Double.class);
    }
}
