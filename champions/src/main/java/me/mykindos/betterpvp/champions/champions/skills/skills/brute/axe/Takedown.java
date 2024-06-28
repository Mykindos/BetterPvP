package me.mykindos.betterpvp.champions.champions.skills.skills.brute.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
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
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Takedown extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DamageSkill, DebuffSkill, MovementSkill {

    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();
    private double damage;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int slownessStrength;
    private double recoilDamage;
    private double recoilDamageIncreasePerLevel;
    private double damageIncreasePerLevel;
    private double velocityStrength;
    private double fallDamageLimit;

    @Inject
    public Takedown(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Takedown";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Hurl yourself forwards, dealing " + getValueString(this::getDamage, level) + " damage,",
                "taking " + getValueString(this::getRecoilDamage, level) + " damage, and applying <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect>",
                "to yourself and the target for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "Cannot be used while grounded",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getRecoilDamage(int level) {
        return recoilDamage + ((level - 1) * recoilDamageIncreasePerLevel);
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


    @UpdateEvent
    public void checkCollision() {

        Iterator<Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {

            Entry<Player, Long> next = it.next();
            Player player = next.getKey();
            if (player.isDead()) {
                it.remove();
                continue;
            }

            final Location midpoint = UtilPlayer.getMidpoint(player).clone();

            final Optional<LivingEntity> hit = UtilEntity.interpolateCollision(midpoint,
                            midpoint.clone().add(player.getVelocity().normalize().multiply(0.5)),
                            (float) 0.6,
                            ent -> UtilEntity.IS_ENEMY.test(player, ent))
                    .map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast);

            if (hit.isPresent()) {
                it.remove();
                doTakedown(player, hit.get());
                continue;
            }


            if (UtilBlock.isGrounded(player) && UtilTime.elapsed(next.getValue(), 750L)) {
                it.remove();
            }
        }

    }

    public void doTakedown(Player player, LivingEntity target) {
        int level = getLevel(player);

        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt>" + target.getName() + "</alt> with <alt>" + getName() + " " + level);
        UtilDamage.doCustomDamage(new CustomDamageEvent(target, player, null, DamageCause.CUSTOM, getDamage(level), false, "Takedown"));

        UtilMessage.simpleMessage(target, getClassType().getName(), "<alt>" + player.getName() + "</alt> hit you with <alt>" + getName() + " " + level);
        UtilDamage.doCustomDamage(new CustomDamageEvent(player, target, null, DamageCause.CUSTOM, getRecoilDamage(level), false, "Takedown Recoil"));

        long duration = (long) (getDuration(level) * 1000L);
        championsManager.getEffects().addEffect(player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(target, player, EffectTypes.NO_JUMP, duration);
        championsManager.getEffects().addEffect(player, EffectTypes.SLOWNESS, slownessStrength, duration);
        championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, slownessStrength, duration);
    }

    @Override
    public boolean canUse(Player p) {

        if (UtilBlock.isGrounded(p)) {
            UtilMessage.simpleMessage(p, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> while grounded.");
            return false;
        }

        return true;
    }

    @Override
    public void activate(Player player, int leel) {
        Vector vec = player.getLocation().getDirection();
        VelocityData velocityData = new VelocityData(vec, velocityStrength, false, 0.0D, 0.4D, 0.6D, false);
        UtilVelocity.velocity(player, null, velocityData, VelocityType.CUSTOM);
        UtilServer.runTaskLater(champions, () -> {
            championsManager.getEffects().addEffect(player, player, EffectTypes.NO_FALL,getName(), (int) fallDamageLimit,
                    50L, true, true, UtilBlock::isGrounded);
        }, 3L);
        active.put(player, System.currentTimeMillis());
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 5.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 4, Integer.class);
        recoilDamage = getConfig("recoilDamage", 1.5, Double.class);
        recoilDamageIncreasePerLevel = getConfig("recoilDamageIncreasePerLevel", 0.5, Double.class);
        velocityStrength = getConfig("velocityStrength", 1.5, Double.class);
        fallDamageLimit = getConfig("fallDamageLimit", 4.0, Double.class);
    }
}
