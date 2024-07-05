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
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
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
import java.util.Random;
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
    private final HashMap<UUID, Long> active = new HashMap<>();
    private final Map<UUID, Boolean> grounded = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Boolean> canHit = new HashMap<>();

    @Inject
    public FeatherFeet(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
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
                "Become extremely light, gaining <effect>Jump Boost " + UtilFormat.getRomanNumeral(getSlowStrength(level)) +"</effect>",
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
        if (!active.containsKey(player.getUniqueId())) {
            championsManager.getEffects().addEffect(player, EffectTypes.JUMP_BOOST, getName(), getJumpBoostStrength(level), (long) (getDuration(level) * 1000));
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.75F);
            active.put(player.getUniqueId(), (long) (System.currentTimeMillis() + (getDuration(level) * 1000L)));
            grounded.put(player.getUniqueId(), false);
            canHit.put(player.getUniqueId(), false);
        }
    }

    @UpdateEvent
    private void spawnSkillParticles(Player player) {
        Random random = UtilMath.RANDOM;
        double dx = (random.nextDouble() - 0.5) * 0.5;
        double dy = (random.nextDouble() - 0.5) * 0.9;
        double dz = (random.nextDouble() - 0.5) * 0.5;

        Location particleLocation = player.getLocation().clone().add(dx, dy, dz);

        double red = 0.4;
        double green = 1.0;
        double blue = 0.4;

        new ParticleBuilder(Particle.SPELL_MOB)
                .location(particleLocation)
                .count(0)
                .offset(red, green, blue)
                .extra(1.0)
                .receivers(60)
                .spawn();
    }

    @UpdateEvent
    public void checkCollision() {
        Iterator<Map.Entry<UUID, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<UUID, Long> next = it.next();
            UUID uuid = next.getKey();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || player.isDead() || System.currentTimeMillis() > next.getValue()) {
                it.remove();
                grounded.remove(uuid);
                canHit.remove(uuid);
                lastDamageTime.remove(uuid);
                continue;
            }

            spawnSkillParticles(player);

            if (UtilBlock.isGrounded(player)) {
                grounded.put(player.getUniqueId(), true);
                canHit.put(player.getUniqueId(), false);
            } else {
                boolean wasGrounded = grounded.getOrDefault(player.getUniqueId(), false);
                grounded.put(player.getUniqueId(), false);

                if (wasGrounded) {
                    canHit.put(player.getUniqueId(), false);
                } else {
                    if (player.getVelocity().getY() < 0 && !canHit.get(player.getUniqueId())) {
                        canHit.put(player.getUniqueId(), true);
                    }
                }
            }

            if (canHit.get(player.getUniqueId())) {
                final Location midpoint = UtilPlayer.getMidpoint(player).clone();
                final Location endPoint = midpoint.clone().add(player.getVelocity().normalize().multiply(0.5));

                if (!Double.isFinite(endPoint.getX()) || !Double.isFinite(endPoint.getY()) || !Double.isFinite(endPoint.getZ())) {
                    continue;
                }

                final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(midpoint,
                                endPoint,
                                (float) 0.6,
                                ent -> UtilEntity.IS_ENEMY.test(player, ent))
                        .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

                if (hit.isPresent()) {
                    LivingEntity target = hit.get();
                    long currentTime = System.currentTimeMillis();
                    if (!lastDamageTime.containsKey(target.getUniqueId()) || currentTime - lastDamageTime.get(target.getUniqueId()) > (damageDelay * 1000)) {
                        lastDamageTime.put(target.getUniqueId(), currentTime);
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
        if(!(event.getDamagee() instanceof Player player)) return;
        if(event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if(!hasSkill(player)) return;
        if (!active.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
    }

    @Override
    public void loadSkillConfig() {
        cooldownDecreasePerLevel = getConfig("cooldownDecreasePerLevel", 1.0, Double.class);
        damage = getConfig("damage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        jumpBoostStrength = getConfig("jumpBoostStrength", 6, Integer.class);
        jumpBoostStrengthIncreasePerLevel = getConfig("jumpBoostStrengthIncreasePerLevel", 0, Integer.class);
        duration = getConfig("duration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        damageDelay = getConfig("damageDelay", 0.5, Double.class);
        cooldown = getConfig("cooldown", 20.0, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);
        slowStrengthIncreasePerLevel = getConfig("slowStrengthIncreasePerLevel", 1, Integer.class);
        slowDuration = getConfig("slowDuration", 3.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.5, Double.class);
    }
}
