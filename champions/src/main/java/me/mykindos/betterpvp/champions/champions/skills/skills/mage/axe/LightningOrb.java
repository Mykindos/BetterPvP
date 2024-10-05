package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

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
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class LightningOrb extends Skill implements InteractSkill, CooldownSkill, Listener, ThrowableListener, OffensiveSkill, DamageSkill, DebuffSkill {

    private double delay;
    private double baseRadius;
    private double delayDecreasePerLevel;
    private double radiusIncreasePerLevel;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private int slowStrength;
    private double baseShockDuration;
    private double shockDurationIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double velocityStrength;

    @Inject
    public LightningOrb(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Lightning Orb";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Launch an electric orb that upon directly hitting a player",
                "or after " + getValueString(this::getDelay, level) + " seconds will strike enemies within " + getValueString(this::getRadius, level) + " blocks",
                "with lightning, dealing " + getValueString(this::getDamage, level) + " damage, <effect>Shocking</effect> them for " + getValueString(this::getShockDuration, level) + " seconds, and",
                "giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.SHOCK.getDescription(0),
        };
    }

    public double getDelay(int level) {
        return delay - ((level - 1) * delayDecreasePerLevel);
    }

    public double getRadius(int level) {
        return baseRadius + (level - 1) * radiusIncreasePerLevel;
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    public double getShockDuration(int level) {
        return baseShockDuration + ((level - 1) * shockDurationIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.MAGE;
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
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        Player playerThrower = (Player) thrower;

        int level = getLevel(playerThrower);
        if (level > 0) {
            activateOrb(playerThrower, throwableItem, level);
        }

        throwableItem.getItem().remove();
    }

    @Override
    public void onTick(ThrowableItem throwableItem) {
        if ((throwableItem.getAge() / 50) > getDelay(getLevel((Player) throwableItem.getThrower())) * 20) {
            activateOrb((Player) throwableItem.getThrower(), throwableItem, getLevel((Player) throwableItem.getThrower()));
            throwableItem.getItem().remove();
        } else {
            throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.6f);
            throwableItem.getLastLocation().getWorld().spawnParticle(Particle.FIREWORK, throwableItem.getLastLocation(), 1);
        }
    }

    private void activateOrb(Player playerThrower, ThrowableItem throwableItem, int level) {
        for (LivingEntity ent : UtilEntity.getNearbyEnemies(playerThrower, throwableItem.getItem().getLocation(), getRadius(level))) {
            if (!throwableItem.getImmunes().contains(ent) && ent.hasLineOfSight(throwableItem.getItem().getLocation())) {
                championsManager.getEffects().addEffect(ent, playerThrower, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000));
                championsManager.getEffects().addEffect(ent, EffectTypes.SHOCK, (long) (getShockDuration(level) * 1000));
                playerThrower.getLocation().getWorld().strikeLightning(ent.getLocation());
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, playerThrower, null, DamageCause.CUSTOM, getDamage(level), false, getName()));
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        Item orb = player.getWorld().dropItem(player.getEyeLocation().add(player.getLocation().getDirection().multiply(velocityStrength)), new ItemStack(Material.DIAMOND_BLOCK));
        orb.setVelocity(player.getLocation().getDirection());
        orb.setCanPlayerPickup(false);
        orb.setCanMobPickup(false);
        ThrowableItem throwableItem = new ThrowableItem(this, orb, player, "Lightning Orb", 5000, false);
        championsManager.getThrowables().addThrowable(throwableItem);
        throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.ENTITY_SILVERFISH_HURT, 2f, 1f);
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("radiusDistance", 3.5, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 0.5, Double.class);

        baseSlowDuration = getConfig("slowDuration", 4.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);

        baseShockDuration = getConfig("baseShockDuration", 2.0, Double.class);
        shockDurationIncreasePerLevel = getConfig("shockDurationIncreasePerLevel", 0.0, Double.class);

        baseDamage = getConfig("baseDamage", 7.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);

        velocityStrength = getConfig("velocityStrength", 3.0, Double.class);

        delay = getConfig("delay", 3.0, Double.class);
        delayDecreasePerLevel = getConfig("delayDecreasePerLevel", 0.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}