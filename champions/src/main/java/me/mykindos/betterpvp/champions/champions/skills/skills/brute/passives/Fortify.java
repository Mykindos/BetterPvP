package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Fortify extends Skill implements PassiveSkill, DefensiveSkill {

    public int base;
    public int increasePerLevel;

    @Inject
    public Fortify(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fortify";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You take " + getValueString(this::getPercent, level, 1, "%", 0) + " less damage",
                "but you deal " + getValueString(this::getPercent, level, 1, "%", 0) + " less damage as well"
        };
    }

    private int getPercent(int level) {
        return (base + (level - 1) * increasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            int level = getLevel(damagee);
            if (level > 0) {
                doParticles(damagee);
                double modifier = getPercent(level);
                event.setDamage(event.getDamage() * (1.0 - (modifier / 100)));
            }
        }


        if (event.getDamager() instanceof Player damager) {
            int level = getLevel(damager);
            if (level > 0) {
                double modifier = getPercent(level);
                event.setDamage(event.getDamage() * (1.0 - (modifier / 100)));
            }

        }
    }

    private void doParticles(Player player) {
        double xLimit = 0.4;
        double yLimit = 0.4;
        double zLimit = 0.4;

        for (int i = 0; i <= 10; i++) {
            double x = UtilMath.randDouble(-xLimit, xLimit);
            double y = UtilMath.randDouble(-yLimit, yLimit);
            double z = UtilMath.randDouble(-zLimit, zLimit);
            Particle.DUST.builder()
                    .location(player.getLocation().add(x, 1 + y, z))
                    .receivers(30)
                    .data(new Particle.DustOptions(Color.BLUE, 1))
                    .spawn();
        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        base = getConfig("base", 10, Integer.class);
        increasePerLevel = getConfig("increasePerLevel", 10, Integer.class);
    }
}
