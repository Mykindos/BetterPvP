package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.damage.ModifierOperation;
import me.mykindos.betterpvp.core.combat.damage.ModifierType;
import me.mykindos.betterpvp.core.combat.damage.ModifierValue;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Fortify extends Skill implements PassiveSkill, DefensiveSkill {

    public int base;
    public int increasePerLevel;

    final int numParticles;
    final double particleRadius;
    private final double[] x;
    private final double[] z;


    @Inject
    public Fortify(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        numParticles = 8;
        particleRadius = 0.5;

        x = new double[numParticles];
        z = new double[numParticles];

        for (int i = 0; i < this.numParticles; i++) {
            final double angleValue = Math.toRadians(((double)i/this.numParticles) * 360);
            this.x[i] = Math.cos(angleValue) * this.particleRadius;
            this.z[i] = Math.sin(angleValue) * this.particleRadius;
        }

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
                // Add a percentage-based damage reduction modifier
                event.getDamageModifiers().addModifier(ModifierType.DAMAGE, modifier, "Fortify", ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
            }
        }


        if (event.getDamager() instanceof Player damager) {
            int level = getLevel(damager);
            if (level > 0) {
                double modifier = getPercent(level);
                // Add a percentage-based damage reduction modifier
                event.getDamageModifiers().addModifier(ModifierType.DAMAGE, modifier, "Fortify", ModifierValue.PERCENTAGE, ModifierOperation.DECREASE);
            }

        }
    }

    private void doParticles(Player player) {
        final Location location = player.getLocation();

        for (int i = 0; i < this.numParticles; i++) {
            Particle.DUST.builder()
                    .count(1)
                    .location(location.clone().add(x[i], player.getHeight()/2, z[i]))
                    .color(Color.fromRGB(81, 184, 172), 0.5f)
                    .receivers(16)
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
