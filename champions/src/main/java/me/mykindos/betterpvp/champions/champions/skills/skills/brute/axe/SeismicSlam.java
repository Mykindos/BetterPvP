package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.brute.data.SeismicSlamData;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.scheduler.BPVPTask;
import me.mykindos.betterpvp.core.scheduler.TaskScheduler;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class SeismicSlam extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, MovementSkill, CrowdControlSkill, DamageSkill {

    private final TaskScheduler taskScheduler;

    private final Map<Player, SeismicSlamData> slams = new WeakHashMap<>();

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double bonusDamagePerTenBlocks;

    @Inject
    public SeismicSlam(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }


    @Override
    public String getName() {
        return "Seismic Slam";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Leap up and slam into the ground, knocking up",
                "players within " + getValueString(this::getRadius, level) + " blocks",
                "and dealing " + getValueString(this::getSlamDamage, level) + " damage",
                "",
                "For every 10 blocks vertically travelled,",
                "deal an additional " + getValueString(this::getBonusDamagePerTenBlocks, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getSlamDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getBonusDamagePerTenBlocks(int level) {
        return bonusDamagePerTenBlocks;
    }

    public double getRadius(int level) {
        return baseRadius + (radiusIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @UpdateEvent
    public void onUpdate() {
        // Existing slam logic
        Iterator<Map.Entry<Player, SeismicSlamData>> slamIterator = slams.entrySet().iterator();
        while (slamIterator.hasNext()) {
            Map.Entry<Player, SeismicSlamData> entry = slamIterator.next();
            Player player = entry.getKey();

            if (player != null) {
                boolean isPlayerGrounded = UtilBlock.isGrounded(player) || player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid();;

                if (isPlayerGrounded) {
                    slam(player, entry.getValue());
                    slamIterator.remove();
                }
            } else {
                slamIterator.remove();
            }
        }

    }


    public void slam(final Player player, SeismicSlamData data) {

        int level = getLevel(player);
        List<LivingEntity> targets = UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius(level));

        for (LivingEntity target : targets) {
            if (target.equals(player)) {
                continue;
            }

            if (target.getLocation().getY() - player.getLocation().getY() >= 3) {
                continue;
            }
            double percentageMultiplier = 1 - (UtilMath.offset(player, target) / getRadius(level));

            double scaledVelocity = 0.6 + percentageMultiplier * 0.4;
            Vector trajectory = UtilVelocity.getTrajectory2d(player.getLocation().toVector(), target.getLocation().toVector());
            VelocityData velocityData = new VelocityData(trajectory, scaledVelocity, true, 0, 0.2 + percentageMultiplier / 1.2, 1, true);
            UtilVelocity.velocity(target, player, velocityData);

            double damage = calculateDamage(player, target, data);
            UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, damage, false, getName()));
            if (target instanceof Player damagee) {
                UtilMessage.message(damagee, getClassType().getPrefix(), UtilMessage.deserialize("<yellow>%s</yellow> hit you with <green>%s %s</green>", player.getName(), getName(), level));
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 0.2f);
        for (Block cur : UtilBlock.getInRadius(player.getLocation(), 4d).keySet()) {
            if (UtilBlock.airFoliage(cur.getRelative(BlockFace.UP)) && !UtilBlock.airFoliage(cur)) {
                cur.getWorld().playEffect(cur.getLocation(), Effect.STEP_SOUND, cur.getType().createBlockData());
            }
        }
    }

    public double calculateDamage(Player player, LivingEntity target, SeismicSlamData data) {
        int level = getLevel(player);
        double minDamage = getSlamDamage(level) / 4;
        double maxDistance = getRadius(level);

        double distance = player.getLocation().distance(target.getLocation());
        double distanceFactor = 1 - (distance / maxDistance);
        distanceFactor = Math.max(0, Math.min(distanceFactor, 1));

        double verticalDistanceBonus = (Math.abs(data.getMaxY() - target.getLocation().getY()) / 10) * bonusDamagePerTenBlocks;

        return minDamage + (verticalDistanceBonus + (getSlamDamage(level) - minDamage)) * distanceFactor;
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
        Vector vec = new Vector(0, 1.3, 0);
        VelocityData velocityData = new VelocityData(vec, 1, false, 0, 1.0, 1, true);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 0.5f);
        Particle.CLOUD.builder()
                .count(20)
                .location(player.getLocation().clone().add(0.0, 0.1, 0.0))
                .receivers(60)
                .extra(0.078)
                .spawn();



        new BukkitRunnable() {

            @Override
            public void run() {

                slams.put(player, new SeismicSlamData(player.getLocation().getBlockY()));

                player.setVelocity(player.getLocation().getDirection().multiply(1.3).add(new Vector(0, -0.5, 0)));
                championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL, "Seismic Slam", 9999, 100, true, true, UtilBlock::isGrounded);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 0.7f);

            }

        }.runTaskLater(champions, 15);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 5.5, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        bonusDamagePerTenBlocks = getConfig("bonusDamagePerTenBlocks", 1.0, Double.class);
    }
}
