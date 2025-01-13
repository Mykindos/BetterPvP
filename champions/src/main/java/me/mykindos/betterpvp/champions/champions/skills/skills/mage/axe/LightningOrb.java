package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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

    @Getter
    private double delay;
    @Getter
    private double radius;
    @Getter
    private double slowDuration;
    private int slowStrength;
    @Getter
    private double shockDuration;
    @Getter
    private double damage;
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
    public String[] getDescription() {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Launch an electric orb that upon directly hitting a player",
                "or after <val>" + getDelay() + "</val> seconds will strike enemies within <val>" + getRadius() + "</val> blocks",
                "with lightning, dealing <val>" + getDamage() + "</val> damage, <effect>Shocking</effect> them for <val>" + getShockDuration(),
                "seconds, and giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for <val>" + getSlowDuration() + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.SHOCK.getDescription(0),
        };
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
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        Player playerThrower = (Player) thrower;

        if (hasSkill(playerThrower)) {
            activateOrb(playerThrower, throwableItem);
        }

        throwableItem.getItem().remove();
    }

    @Override
    public void onTick(ThrowableItem throwableItem) {
        if ((throwableItem.getAge() / 50) > getDelay()) {
            activateOrb((Player) throwableItem.getThrower(), throwableItem);
            throwableItem.getItem().remove();
        } else {
            throwableItem.getLastLocation().getWorld().playSound(throwableItem.getLastLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.6f, 1.6f);
            throwableItem.getLastLocation().getWorld().spawnParticle(Particle.FIREWORK, throwableItem.getLastLocation(), 1);
        }
    }

    private void activateOrb(Player playerThrower, ThrowableItem throwableItem) {
        for (LivingEntity ent : UtilEntity.getNearbyEnemies(playerThrower, throwableItem.getItem().getLocation(), getRadius())) {
            if (!throwableItem.getImmunes().contains(ent) && ent.hasLineOfSight(throwableItem.getItem().getLocation())) {
                championsManager.getEffects().addEffect(ent, playerThrower, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration() * 1000));
                championsManager.getEffects().addEffect(ent, EffectTypes.SHOCK, (long) (getShockDuration() * 1000));
                playerThrower.getLocation().getWorld().strikeLightning(ent.getLocation());
                UtilDamage.doCustomDamage(new CustomDamageEvent(ent, playerThrower, null, DamageCause.CUSTOM, getDamage(), false, getName()));
            }
        }
    }

    @Override
    public void activate(Player player) {
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
        radius = getConfig("radiusDistance", 3.5, Double.class);

        slowDuration = getConfig("slowDuration", 4.0, Double.class);
        slowStrength = getConfig("slowStrength", 2, Integer.class);

        shockDuration = getConfig("shockDuration", 2.0, Double.class);

        damage = getConfig("damage", 7.0, Double.class);

        velocityStrength = getConfig("velocityStrength", 3.0, Double.class);

        delay = getConfig("delay", 3.0, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}