package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Singleton
@BPvPListener
public class ThreateningShout extends Skill implements Listener, InteractSkill, CooldownSkill {

    private double radius;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int vulnerabilityStrength;
    private final Map<Player, List<Location>> playerPointsMap;
    private final Map<Player, Integer> playerPointIndexMap;
    private final Map<Player, Set<LivingEntity>> playerAffectedEntitiesMap;
    private final Map<Player, Set<LivingEntity>> playerDamagedEntitiesMap;
    private int tickDelay;
    private double damage;
    private double damageIncreasePerLevel;
    private double startDistance;

    @Inject
    public ThreateningShout(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        playerPointsMap = new HashMap<>();
        playerPointIndexMap = new HashMap<>();
        playerAffectedEntitiesMap = new HashMap<>();
        playerDamagedEntitiesMap = new HashMap<>();
    }

    @Override
    public String getName() {
        return "Threatening Shout";
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Release a roar, inflicting all enemies hit",
                "with <effect>Vulnerability " + UtilFormat.getRomanNumeral(vulnerabilityStrength) + "</effect> for <val>" + getDuration(level) + "</val> seconds",
                "",
                "After <stat>" + ((float) tickDelay / 20) + "</stat> seconds the shout will explode,",
                "dealing <val>" + getDamage(level) + "</val> damage",
                "",
                "Cooldown: <val>" + getCooldown(level),
                "",
                EffectTypes.VULNERABILITY.getDescription(vulnerabilityStrength)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 2.0F);

        Location start = player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(startDistance));
        List<Location> points = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            Location point = start.clone().add(start.getDirection().normalize().multiply(i * 1.0));
            Block targetBlock = point.getBlock();
            if (!UtilBlock.airFoliage(targetBlock)) {
                break;
            }
            points.add(point);
        }

        playerPointsMap.put(player, points);
        playerPointIndexMap.put(player, 0);
        playerAffectedEntitiesMap.put(player, new HashSet<>());
        playerDamagedEntitiesMap.put(player, new HashSet<>());
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<Map.Entry<Player, List<Location>>> iterator = playerPointsMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Player, List<Location>> entry = iterator.next();
            Player player = entry.getKey();
            int level = getLevel(player);
            List<Location> points = entry.getValue();
            int currentPointIndex = playerPointIndexMap.get(player);
            Set<LivingEntity> affectedEntities = playerAffectedEntitiesMap.get(player);
            Set<LivingEntity> damagedEntities = playerDamagedEntitiesMap.get(player);

            if (points.isEmpty() || currentPointIndex >= points.size()) {
                iterator.remove();
                playerPointIndexMap.remove(player);
                playerAffectedEntitiesMap.remove(player);
                playerDamagedEntitiesMap.remove(player);
                continue;
            }

            for (int i = 0; i < 2 && currentPointIndex < points.size(); i++) {
                Location point = points.get(currentPointIndex);

                point.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 0, 0, 0, 0, 0);

                for (LivingEntity target : UtilEntity.getNearbyEnemies(player, point, radius)) {
                    if (!affectedEntities.contains(target)) {
                        championsManager.getEffects().addEffect(target, EffectTypes.VULNERABILITY, vulnerabilityStrength, (long) (getDuration(level) * 1000L));
                        UtilMessage.message(target, getName(), "<yellow>%s</yellow> gave you <white>Vulnerability</white> for <green>%s</green> seconds.", player.getName(), getDuration(level));
                        UtilMessage.message(player, getName(), "You gave <yellow>%s</yellow> <white>Vulnerability " + UtilFormat.getRomanNumeral(vulnerabilityStrength) + "</white> for <green>%s</green> seconds.", target.getName(), getDuration(level));
                        affectedEntities.add(target);
                    }
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (LivingEntity damageTarget : UtilEntity.getNearbyEnemies(player, point, radius)) {
                            if (affectedEntities.contains(damageTarget) && !damagedEntities.contains(damageTarget)) {
                                UtilDamage.doCustomDamage(new CustomDamageEvent(damageTarget, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Threatening Shout"));
                                UtilMessage.message(player, getName(), "You hit <yellow>%s</yellow> with <green>Threatening Shout</green>", damageTarget.getName());
                                damagedEntities.add(damageTarget);
                            }
                        }
                    }
                }.runTaskLater(champions, tickDelay);

                currentPointIndex++;
            }

            playerPointIndexMap.put(player, currentPointIndex);
        }
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 3.0, Double.class);
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        vulnerabilityStrength = getConfig("vulnerabilityStrength", 3, Integer.class);
        tickDelay = getConfig("tickDelay", 12, Integer.class);
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        startDistance = getConfig("startDistance", 1.0, Double.class);
    }
}
