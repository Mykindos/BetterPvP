package me.mykindos.betterpvp.champions.champions.skills.skills.mage.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class Pestilence extends PrepareSkill implements CooldownSkill {

    private final HashMap<UUID, PestilenceData> pestilenceData = new HashMap<>();

    private double infectionDuration;
    private double enemyDamageReduction;

    private double radius;

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
                "Your next hit will apply <effect>Pestilence</effect> to the target,",
                "<effect>Poisoning</effect> them, and spreading to nearby enemies",
                "",
                "While enemies are infected, they",
                "deal <stat>" + (enemyDamageReduction * 100) + "%</stat> reduced damage",
                "",
                "<effect>Pestilence</effect> lasts <stat>" + infectionDuration + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @UpdateEvent(delay = 500)
    public void spread() {
        pestilenceData.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);

        pestilenceData.forEach((key, value) -> {
            Player player = Bukkit.getPlayer(key);
            if (player == null) {
                value.getOldInfected().clear();
                value.getCurrentlyInfected().clear();
                return;
            }

            for (LivingEntity entity : value.getCurrentlyInfected().keySet()) {
                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, entity.getLocation(), radius)) {
                    if (value.getCurrentlyInfected().containsKey(target)) continue;
                    if (value.getOldInfected().containsKey(target)) continue;

                    value.addInfection(target, (long) infectionDuration * 1000);
                }
            }

        });

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
        if (!UtilPlayer.isHoldingItem(damager, SkillWeapons.SWORDS)) return;
        if (!active.contains(damager.getUniqueId())) return;

        int level = getLevel(damager);
        if (level > 0) {
            PestilenceData data = new PestilenceData();
            data.addInfection(event.getDamagee(), (long) infectionDuration * 1000);
            pestilenceData.put(damager.getUniqueId(), data);
            active.remove(damager.getUniqueId());
        }

    }

    @EventHandler
    public void onDamageReduction(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!event.getDamager().hasPotionEffect(PotionEffectType.POISON)) return;

        if (isInfected(event.getDamager())) {
            event.setDamage(event.getDamage() * (1 - enemyDamageReduction));
        }

    }

    public boolean isInfected(LivingEntity entity) {
        return pestilenceData.values().stream().anyMatch(value -> value.currentlyInfected.containsKey(entity));
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
        infectionDuration = getConfig("duration", 5.0, Double.class);
        enemyDamageReduction = getConfig("enemyDamageReduction", 0.20, Double.class);
        radius = getConfig("radius", 5.0, Double.class);
    }

    @Data
    private static class PestilenceData {

        private final HashMap<LivingEntity, DamageData> oldInfected = new HashMap<>();
        private final HashMap<LivingEntity, DamageData> currentlyInfected = new HashMap<>();

        public void addInfection(LivingEntity entity, long length) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (int) ((length / 1000) * 20), 0));
            currentlyInfected.put(entity, new DamageData(length));
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

            public DamageData(long length) {
                this.startTime = System.currentTimeMillis();
                this.length = length;
            }

        }

    }
}

