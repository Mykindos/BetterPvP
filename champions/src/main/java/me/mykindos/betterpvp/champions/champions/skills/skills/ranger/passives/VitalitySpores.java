package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

@Singleton
@BPvPListener
public class VitalitySpores extends Skill implements PassiveSkill, DefensiveSkill, HealthSkill {

    private final WeakHashMap<LivingEntity, List<SporeCharge>> sporeCharges = new WeakHashMap<>();
    private double sporeRemovalTime;
    private int maxSporeCharges;
    private int maxSporeChargesIncreasePerLevel;
    private double healing;
    private double healingIncreasePerLevel;

    private static class SporeCharge {
        double xOffset;
        double yOffset;
        double zOffset;
        long appliedTime;
        Player applier;

        SporeCharge(double xOffset, double yOffset, double zOffset, long appliedTime, Player applier) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.appliedTime = appliedTime;
            this.applier = applier;
        }
    }

    @Inject
    public VitalitySpores(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vitality Spores";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Players hit with your arrows will receive a spore charge",
                "each time you hit someone with a spore charge you will",
                "heal " + getValueString(this::getHealing, level) + " health",
                "",
                "Maximum spore charges: " + getValueString(this::getMaxSporeCharges, level),
                "",
                "Expires after " + getValueString(this::getSporeRemovalTime, level) + " seconds",
        };
    }

    public double getSporeRemovalTime(int level) {
        return sporeRemovalTime;
    }

    public double getHealing(int level){
        return healing + ((level - 1) * healingIncreasePerLevel);
    }

    public int getMaxSporeCharges(int level) {
        return maxSporeCharges + ((level - 1) * maxSporeChargesIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow)) return;
        LivingEntity target = event.getDamagee();

        int level = getLevel(player);
        if (level > 0) {
            sporeCharges.putIfAbsent(target, new ArrayList<>());
            List<SporeCharge> currentCharges = sporeCharges.get(target);
            if (currentCharges.size() < getMaxSporeCharges(level)) {
                double xOffset = (Math.random() - 0.5) * 2;
                double yOffset = Math.random() * 2;
                double zOffset = (Math.random() - 0.5) * 2;
                currentCharges.add(new SporeCharge(xOffset, yOffset, zOffset, System.currentTimeMillis(), player));
            }
        }
    }

    @EventHandler
    public void onMeleeHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        LivingEntity target = event.getDamagee();

        if (sporeCharges.containsKey(target) && !sporeCharges.get(target).isEmpty()) {
            int level = getLevel(player);
            List<SporeCharge> currentCharges = sporeCharges.get(target);
            if (level > 0 && !currentCharges.isEmpty()) {
                UtilPlayer.health(player, getHealing(level));
                currentCharges.removeFirst();
                if (currentCharges.isEmpty()) {
                    sporeCharges.remove(target);
                }
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
            }
        }
    }

    @UpdateEvent
    public void updateVitalityData() {
        long currentTime = System.currentTimeMillis();

        sporeCharges.forEach((entity, charges) -> {
            charges.removeIf(charge -> {
                if (currentTime - charge.appliedTime > getSporeRemovalTime(getLevel(charge.applier)) * 1000) {
                    return true;
                } else {
                    Location particleLocation = entity.getLocation().add(charge.xOffset, charge.yOffset, charge.zOffset);
                    new ParticleBuilder(Particle.DUST)
                            .location(particleLocation)
                            .count(1)
                            .offset(0.0, 0.0, 0.0)
                            .data(new Particle.DustOptions(org.bukkit.Color.fromRGB(0, 255, 0), 0.5f))
                            .receivers(30)
                            .spawn();
                    return false;
                }
            });
        });
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        maxSporeCharges = getConfig("maxSporeCharges", 2, Integer.class);
        maxSporeChargesIncreasePerLevel = getConfig("maxSporeChargesIncreasePerLevel", 1, Integer.class);
        sporeRemovalTime = getConfig("sporeRemovalTime", 5.0, Double.class);
        healingIncreasePerLevel = getConfig("healingIncreasePerLevel", 0.0, Double.class);
        healing = getConfig("healing", 1.5, Double.class);
    }
}
