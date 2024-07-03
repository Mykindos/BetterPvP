package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
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
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class SeismicSlam extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, MovementSkill, CrowdControlSkill, DamageSkill {
    private final HashMap<UUID, Long> slams = new HashMap<>();

    private double baseRadius;
    private double radiusIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private int slamDelay;
    private double fallDamageLimit;

    @Inject
    public SeismicSlam(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
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
                "Leap up and slam into the ground, causing",
                "players within " + getValueString(this::getRadius, level) + " blocks to fly",
                "upwards and take " + getValueString(this::getSlamDamage, level) + " damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getSlamDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
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
        Iterator<Map.Entry<UUID, Long>> slamIterator = slams.entrySet().iterator();
        while (slamIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = slamIterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player != null) {
                long activationTime = entry.getValue();
                boolean timeElapsed = UtilTime.elapsed(activationTime, slamDelay);
                boolean isPlayerGrounded = UtilBlock.isGrounded(player) || player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid();

                if (timeElapsed && isPlayerGrounded) {
                    slam(player);
                    slamIterator.remove();
                }
            } else {
                slamIterator.remove();
            }
        }

    }


    public void slam(final Player player) {

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

            double damage = calculateDamage(player, target);
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

    public double calculateDamage(Player player, LivingEntity target) {
        int level = getLevel(player);
        double minDamage = getSlamDamage(level) / 4;
        double maxDistance = getRadius(level);

        double distance = player.getLocation().distance(target.getLocation());
        double distanceFactor = 1 - (distance / maxDistance);
        distanceFactor = Math.max(0, Math.min(distanceFactor, 1));

        return minDamage + (getSlamDamage(level) - minDamage) * distanceFactor;
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
        Vector vec = player.getLocation().getDirection();
        if (vec.getY() < 0) {
            vec.setY(vec.getY() * -1);
        }

        VelocityData velocityData = new VelocityData(vec, 0.6, false, 0, 0.8, 0.8, true);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);

        slams.put(player.getUniqueId(), System.currentTimeMillis());
        UtilServer.runTaskLater(champions, () -> {
            championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                    50L, true, true, UtilBlock::isGrounded);
        }, 3L);
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
        slamDelay = getConfig("slamDelay", 500, Integer.class);
        fallDamageLimit = getConfig("fallDamageLimit", 20.0, Double.class);
    }
}
