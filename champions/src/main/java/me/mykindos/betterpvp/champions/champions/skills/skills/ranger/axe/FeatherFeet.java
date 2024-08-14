package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
@BPvPListener
public class FeatherFeet extends Skill implements InteractSkill, CooldownSkill, Listener, BuffSkill, MovementSkill, DamageSkill {

    private double cooldownDecreasePerLevel;
    private double damageIncreasePerLevel;
    private double damage;
    private int jumpBoostStrength;
    private int jumpBoostStrengthIncreasePerLevel;
    private double duration;
    private double durationIncreasePerLevel;
    private double damageDelay;
    private int slowStrength;
    private int slowStrengthIncreasePerLevel;
    private double slowDuration;
    private double slowDurationIncreasePerLevel;
    private final Map<UUID, FeatherFeetData> dataMap = new HashMap<>();
    private final TaskScheduler taskScheduler;

    @Inject
    public FeatherFeet(Champions champions, ChampionsManager championsManager, TaskScheduler taskScheduler) {
        super(champions, championsManager);
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getName() {
        return "Feather Feet";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Become extremely light, gaining <effect>Jump Boost " + UtilFormat.getRomanNumeral(getJumpBoostStrength(level)) +"</effect>",
                "and taking no fall damage for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Landing on players will deal " + getValueString(this::getDamage, level) + " damage",
                "and give them <effect>Slowness " + UtilFormat.getRomanNumeral(getSlowStrength(level)) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
        };
    }

    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getSlowDuration(int level){
        return slowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    public int getSlowStrength(int level){
        return slowStrength + ((level - 1) * slowStrengthIncreasePerLevel);
    }

    public double getDuration(int level) {
        return duration + ((level - 1) * durationIncreasePerLevel);
    }

    public int getJumpBoostStrength(int level){
        return jumpBoostStrength + ((level - 1) * jumpBoostStrengthIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public void activate(Player player, int level) {
        if (!dataMap.containsKey(player.getUniqueId())) {
            championsManager.getEffects().addEffect(player, EffectTypes.JUMP_BOOST, getName(), getJumpBoostStrength(level), (long) (getDuration(level) * 1000));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.75F);
            FeatherFeetData data = new FeatherFeetData((long)(System.currentTimeMillis() + (getDuration(level) * 1000L)), false, 0L, false);
            dataMap.put(player.getUniqueId(), data);
        }
    }

    @UpdateEvent
    private void spawnSkillParticles(Player player) {

        Location loc = player.getLocation();

        new ParticleBuilder(Particle.SMALL_GUST)
                .location(loc)
                .count(3)
                .offset(0.3, 0.6, 0.3)
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @UpdateEvent
    public void checkCollision() {
        Iterator<Map.Entry<UUID, FeatherFeetData>> it = dataMap.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<UUID, FeatherFeetData> next = it.next();
            UUID uuid1 = next.getKey();
            Player player = Bukkit.getPlayer(uuid1);
            FeatherFeetData data = next.getValue();

            if (player == null || player.isDead() || System.currentTimeMillis() > data.getEndTime()) {
                it.remove();
                if(player != null) {
                    taskScheduler.addTask(new BPVPTask(player.getUniqueId(), uuid -> !UtilBlock.isGrounded(uuid), uuid -> {
                        Player target = Bukkit.getPlayer(uuid);
                        if(target != null) {
                            championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), 1000,
                                    50L, true, true, UtilBlock::isGrounded);
                        }
                    }, 1000));
                }
                return;
            }

            spawnSkillParticles(player);

            if (UtilBlock.isGrounded(player)) {
                data.setGrounded(true);
                data.setCanHit(false);
            } else {
                boolean wasGrounded = data.isGrounded();
                data.setGrounded(false);

                if (wasGrounded) {
                    data.setCanHit(false);
                } else {
                    if (player.getVelocity().getY() < 0 && !data.isCanHit()) {
                        data.setCanHit(true);
                    }
                }
            }

            if (data.isCanHit()) {
                final Location midpoint = UtilPlayer.getMidpoint(player).clone();
                final Location endPoint = midpoint.clone().add(player.getVelocity().normalize().multiply(0.5));

                if (!Double.isFinite(endPoint.getX()) || !Double.isFinite(endPoint.getY()) || !Double.isFinite(endPoint.getZ())) {
                    continue;
                }

                final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(midpoint,
                                endPoint,
                                (float) 1.0,
                                ent -> UtilEntity.IS_ENEMY.test(player, ent))
                        .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

                if (hit.isPresent()) {
                    LivingEntity target = hit.get();
                    long currentTime = System.currentTimeMillis();
                    if (data.getLastDamageTime() == 0 || currentTime - data.getLastDamageTime() > (damageDelay * 1000)) {
                        data.setLastDamageTime(currentTime);
                        doCollision(player, target);
                    }
                }
            }
        }
    }

    public void doCollision(Player player, LivingEntity target) {
        int level = getLevel(player);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>" + target.getName() + "</alt2> with <alt>" + getName() + " " + level);
        UtilMessage.simpleMessage(target, getClassType().getName(), "<alt2>" + player.getName() + "</alt2> hit you with <alt>" + getName() + " " + level);

        UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, "Feather Feet"));
        championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, getSlowStrength(level), (long) (getSlowDuration(level) * 1000));

    }

    @EventHandler
    public void stopFallDamage(CustomDamageEvent event){
        if (!(event.getDamagee() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!hasSkill(player)) return;
        if (!dataMap.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void modifyKnockback(CustomDamageEvent event){
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!hasSkill(player)) return;
        if (!dataMap.containsKey(player.getUniqueId())) return;
        if (UtilBlock.isGrounded(player, 1)) return;

        event.setKnockback(false);
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        jumpBoostStrength = getConfig("jumpBoostStrength", 5, Integer.class);
        jumpBoostStrengthIncreasePerLevel = getConfig("jumpBoostStrengthIncreasePerLevel", 0, Integer.class);
        duration = getConfig("duration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        damageDelay = getConfig("damageDelay", 0.5, Double.class);
        cooldown = getConfig("cooldown", 20.0, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);
        slowStrengthIncreasePerLevel = getConfig("slowStrengthIncreasePerLevel", 0, Integer.class);
        slowDuration = getConfig("slowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
    }
}

class FeatherFeetData {
    private long endTime;
    private boolean grounded;
    private long lastDamageTime;
    private boolean canHit;

    public FeatherFeetData(long endTime, boolean grounded, long lastDamageTime, boolean canHit) {
        this.endTime = endTime;
        this.grounded = grounded;
        this.lastDamageTime = lastDamageTime;
        this.canHit = canHit;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public long getLastDamageTime() {
        return lastDamageTime;
    }

    public void setLastDamageTime(long lastDamageTime) {
        this.lastDamageTime = lastDamageTime;
    }

    public boolean isCanHit() {
        return canHit;
    }

    public void setCanHit(boolean canHit) {
        this.canHit = canHit;
    }
}
