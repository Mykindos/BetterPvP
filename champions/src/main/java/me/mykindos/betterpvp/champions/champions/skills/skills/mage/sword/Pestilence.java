package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
@BPvPListener
public class Pestilence extends PrepareSkill implements CooldownSkill, OffensiveSkill, DebuffSkill {

    private final ConcurrentHashMap<UUID, PestilenceData> pestilenceData = new ConcurrentHashMap<>();

    private double infectionDuration;
    private double infectionDurationIncreasePerLevel;
    private double enemyDamageReduction;
    private double enemyDamageReductionIncreasePerLevel;
    private double radius;
    private double radiusIncreasePerLevel;

    @Inject
    public Pestilence(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Pestilence";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "Your next sword strike will inflict <effect>Pestilence</effect> on the target,",
                "<effect>Poisoning</effect> them, and spreading to nearby enemies",
                "up to " + getValueString(this::getCooldown, level) + "blocks away",
                "",
                "While enemies are infected, they",
                "deal " + getValueString(this::getEnemyDamageReduction, level, 100, "%", 0) + " reduced damage from melee attacks",
                "",
                "<effect>Pestilence</effect> lasts " + getValueString(this::getInfectionDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getEnemyDamageReduction(int level) {
        return enemyDamageReduction + ((level - 1) * enemyDamageReductionIncreasePerLevel);
    }

    public double getInfectionDuration(int level) {
        return infectionDuration + ((level - 1) * infectionDurationIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @UpdateEvent(delay = 500)
    public void spread() {
        Iterator<Map.Entry<UUID, PestilenceData>> iterator = pestilenceData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PestilenceData> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                entry.getValue().getOldInfected().clear();
                entry.getValue().getCurrentlyInfected().clear();
                iterator.remove();
            } else {
                List<LivingEntity> newInfections = new ArrayList<>();
                for (LivingEntity entity : entry.getValue().getCurrentlyInfected().keySet()) {
                    for (LivingEntity target : UtilEntity.getNearbyEnemies(player, entity.getLocation(), radius)) {
                        if (entry.getValue().getCurrentlyInfected().containsKey(target)) continue;
                        if (entry.getValue().getOldInfected().containsKey(target)) continue;

                        newInfections.add(target);
                    }
                }
                for (LivingEntity target : newInfections) {
                    entry.getValue().addInfection(championsManager, target);
                }
            }
        }
    }

    @UpdateEvent(delay = 500)
    public void updatePestilence() {
        pestilenceData.forEach((key, value) -> {
            value.processInfections();
        });
    }

    @UpdateEvent(delay = 1000)
    public void displayPestilence() {
        pestilenceData.forEach((key, value) -> {
            value.currentlyInfected.keySet().forEach(infected -> {
                for (int q = 0; q <= 10; q++) {
                    final float x = (float) (1 * Math.cos(q));
                    final float z = (float) (1 * Math.sin(q));

                    Bukkit.getScheduler().scheduleSyncDelayedTask(champions,
                            () -> Particle.VILLAGER_HAPPY.builder()
                                    .location(infected.getLocation().add(x, 1, z))
                                    .receivers(30)
                                    .extra(0)
                                    .spawn(),
                            q * 5L);

                }
            });
        });

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onApplyInfection(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!isHolding(damager)) return;
        if (!active.contains(damager.getUniqueId())) return;

        int level = getLevel(damager);
        if (level > 0) {
            PestilenceData data = new PestilenceData((long) getInfectionDuration(level) * 1000, getEnemyDamageReduction(level));
            data.addInfection(championsManager, event.getDamagee());
            pestilenceData.put(damager.getUniqueId(), data);
            active.remove(damager.getUniqueId());
        }

    }

    @EventHandler
    public void onDamageReduction(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (event.getDamager() == null) return;

        double reduction = getDamageReduction(event.getDamager());
        event.setDamage(event.getDamage() * (1 - reduction));

    }

    /**
     * Returns the damage reduction, if there are active entries that have the entity, return the highest damage reduction. Defaults to 0.
     * @param entity the entity to get the damage reduction
     * @return damage reduction
     */
    public double getDamageReduction(LivingEntity entity) {
        return pestilenceData.values().stream()
                .filter(value -> value.currentlyInfected.containsKey(entity))
                .map(data -> data.currentlyInfected.get(entity).getDamageReduction())
                .max(Double::compare).orElse(0d);
    }

    @EventHandler
    public void onEffectClear(EffectClearEvent event) {
        pestilenceData.values().forEach(value -> value.getCurrentlyInfected().entrySet().removeIf(entry -> entry.getKey().equals(event.getPlayer())));
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        infectionDuration = getConfig("infectionDuration", 5.0, Double.class);
        infectionDurationIncreasePerLevel = getConfig("infectionDurationIncreasePerLevel", 0.0, Double.class);
        enemyDamageReduction = getConfig("enemyDamageReduction", 0.20, Double.class);
        enemyDamageReductionIncreasePerLevel = getConfig("enemyDamageReductionIncreasePerLevel", 0.0, Double.class);
        radius = getConfig("radius", 5.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.0, Double.class);
    }

    @Data
    private static class PestilenceData {

        private final ConcurrentHashMap<LivingEntity, DamageData> oldInfected = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<LivingEntity, DamageData> currentlyInfected = new ConcurrentHashMap<>();
        private final long length;
        private final double damageReduction;

        public void addInfection(ChampionsManager championsManager, LivingEntity entity) {
            championsManager.getEffects().addEffect(entity, EffectTypes.POISON, 1, length);
            currentlyInfected.put(entity, new DamageData(length, damageReduction));
        }

        public void processInfections() {
            currentlyInfected.forEach((key, value) -> {
                if (UtilTime.elapsed(value.getStartTime(), value.getLength())) {
                    oldInfected.put(key, value);
                }
            });

            currentlyInfected.entrySet().removeIf(entry -> oldInfected.containsKey(entry.getKey()));
            if (currentlyInfected.isEmpty()) {
                oldInfected.clear();
            }
        }

        @Data
        private static class DamageData {

            private final long startTime;
            private final long length;
            private final double damageReduction;

            public DamageData(long length, double damageReduction) {
                this.damageReduction = damageReduction;
                this.startTime = System.currentTimeMillis();
                this.length = length;
            }

        }

    }
}

