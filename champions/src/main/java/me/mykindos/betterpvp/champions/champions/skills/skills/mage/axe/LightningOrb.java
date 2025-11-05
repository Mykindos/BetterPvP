package me.mykindos.betterpvp.champions.champions.skills.skills.mage.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.mage.data.LightningOrbProjectile;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.energy.events.EnergyEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class LightningOrb extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill, DamageSkill, DebuffSkill {

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
    private double projectileHitboxSize;
    private double speed;
    private double energyGain;
    private double energyGainPerLevel;

    private final WeakHashMap<Player, List<LightningOrbProjectile>> projectiles = new WeakHashMap<>();

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
                "Launch an electric orb that upon directly hitting an",
                "enemy will strike enemies within " + getValueString(this::getRadius, level) + " blocks with",
                "lightning, dealing " + getValueString(this::getDamage, level) + " damage, <effect>Shocking</effect> them for " + getValueString(this::getShockDuration, level),
                "seconds, and giving them <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "For every enemy hit, you gain " + getValueString(this::getEnergyGain, level) + " energy",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.SHOCK.getDescription(0),
        };
    }

    public double getEnergyGain(int level) {
        return energyGain + ((level - 1) * energyGainPerLevel);
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

    private void onAttach(Player caster, LivingEntity target, int level) {
        championsManager.getEffects().addEffect(target, caster, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration(level) * 1000));
        championsManager.getEffects().addEffect(target, caster, EffectTypes.SHOCK, (long) (getShockDuration(level) * 1000));
        target.getLocation().getWorld().strikeLightningEffect(target.getLocation());
        championsManager.getEnergy().regenerateEnergy(caster, getEnergyGain(level), EnergyEvent.Cause.USE);
        UtilDamage.doDamage(new DamageEvent(target,
                caster,
                null,
                new SkillDamageCause(this).withBukkitCause(DamageCause.LIGHTNING).withCategory(DamageCauseCategory.RANGED),
                getDamage(level),
                getName()));
        new SoundEffect(Sound.BLOCK_CONDUIT_DEACTIVATE, 0.6f, 1.3f).play(target.getLocation());
    }

    @Override
    public void activate(Player player, int level) {
        LightningOrbProjectile projectile = new LightningOrbProjectile(
                player,
                projectileHitboxSize,
                player.getEyeLocation(),
                (long) (getDelay(level) * 1000L),
                getRadius(level),
                target -> onAttach(player, target, level)
        );
        projectile.redirect(player.getEyeLocation().getDirection().multiply(speed));

        projectiles.computeIfAbsent(player, k -> new ArrayList<>()).add(projectile);
        new SoundEffect(Sound.ENTITY_SILVERFISH_HURT, 1F, 1F).play(player.getEyeLocation());
    }

    @UpdateEvent
    public void updateProjectiles() {
        final Iterator<Player> iterator = projectiles.keySet().iterator();
        while (iterator.hasNext()) {
            final Player player = iterator.next();
            final List<LightningOrbProjectile> projectiles = this.projectiles.get(player);

            if (player == null || !player.isValid() || !player.isOnline() || projectiles == null || projectiles.isEmpty()) {
                iterator.remove();

                if (projectiles != null) {
                    projectiles.forEach(LightningOrbProjectile::remove);
                    projectiles.clear();
                }

                continue;
            }

            final Iterator<LightningOrbProjectile> projectileIterator = projectiles.iterator();
            while (projectileIterator.hasNext()) {
                final LightningOrbProjectile projectile = projectileIterator.next();
                if (projectile.isExpired() || projectile.isMarkForRemoval()) {
                    projectile.remove();
                    projectileIterator.remove();
                    continue;
                }

                // Tick the projectile to update position and visuals
                projectile.tick();
            }
        }
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
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);

        speed = getConfig("speed", 3.0, Double.class);
        projectileHitboxSize = getConfig("projectileHitboxSize", 0.5, Double.class);

        delay = getConfig("delay", 3.0, Double.class);
        delayDecreasePerLevel = getConfig("delayDecreasePerLevel", 0.0, Double.class);

        energyGain = getConfig("energyGain", 0.1, Double.class);
        energyGainPerLevel = getConfig("energyGainPerLevel", 0.05, Double.class);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}