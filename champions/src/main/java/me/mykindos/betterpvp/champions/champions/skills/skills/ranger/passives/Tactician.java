package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerData;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.DaggerDataManager;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class Tactician extends Skill implements PassiveSkill, Listener, DamageSkill, DebuffSkill {

    private double damageIncreasePerLevel;
    private double damage;
    private double hitboxSize;
    private int slowStrengthIncreasePerLevel;
    private int slowStrength;
    private double slowDuration;
    private double slowDurationIncreasePerLevel;
    private double headOffset;

    @Inject
    public Tactician(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Tactician";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Melee hits to the head deal " + getValueString(this::getDamage, level) + " extra damage and",
                "melee hits to the body give <effect>Slowness " + UtilFormat.getRomanNumeral(getSlowStrength(level)) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds"
        };
    }

    private double getDamage(int level) {
        return damage + ((level - 1) * damageIncreasePerLevel);
    }

    private double getSlowDuration(int level){
        return slowDuration + ((level - 1) * slowDurationIncreasePerLevel);
    }

    private int getSlowStrength(int level){
        return slowStrength + ((level - 1) * slowStrengthIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (event.isCancelled()) return;
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
            LivingEntity damagee = event.getDamagee();
            int level = getLevel(damager);
            if (level > 0) {
                Location hitPos;
                DaggerData data = DaggerDataManager.getInstance().getDaggerData(damager);

                if (data != null && data.getHitLocation() != null && event.hasReason("Wind Dagger")) {
                    hitPos = data.getHitLocation();
                } else {
                    RayTraceResult result = damagee.getWorld().rayTraceEntities(
                            damager.getEyeLocation(),
                            damager.getEyeLocation().getDirection(),
                            10.0,
                            hitboxSize,
                            entity -> entity instanceof LivingEntity && !entity.equals(damager)
                    );

                    if (result != null && result.getHitEntity() instanceof LivingEntity hitEntity) {
                        hitPos = result.getHitPosition().toLocation(damager.getEyeLocation().getWorld());
                        hitPos.setX(hitEntity.getLocation().getX());
                        hitPos.setZ(hitEntity.getLocation().getZ());
                    } else {
                        championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, getSlowStrength(level), (long) (getSlowDuration(level) * 1000));
                        event.addReason("Slowness Tactics");
                        return;
                    }
                }

                LivingEntity hitEntity = damagee;
                Location headPos = hitEntity.getEyeLocation().add(0, headOffset, 0);
                Location footPos = hitEntity.getLocation();

                double headDistance = headPos.distance(hitPos);
                double footDistance = footPos.distance(hitPos);

                if (headDistance <= footDistance) {
                    event.setDamage(event.getDamage() + getDamage(level));
                    damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 2.0f);

                    if(damagee instanceof Player){
                        damager.getWorld().playEffect(event.getDamagee().getLocation().add(0, 2 ,0), Effect.STEP_SOUND, Material.REDSTONE_WIRE);
                    } else {
                        damager.getWorld().playEffect(event.getDamagee().getEyeLocation(), Effect.STEP_SOUND, Material.REDSTONE_WIRE);
                    }
                    event.addReason("Decapitation Tactics");

                } else {
                    championsManager.getEffects().addEffect(hitEntity, damager, EffectTypes.SLOWNESS, getSlowStrength(level), (long) (getSlowDuration(level) * 1000));
                    event.addReason("Slowness Tactics");
                }
            }
        }
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 0.4, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.4, Double.class);
        hitboxSize = getConfig("hitboxSize", 1.0, Double.class);
        slowDuration = getConfig("slowDuration", 0.5, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slowStrength = getConfig("slowStrength", 1, Integer.class);
        slowStrengthIncreasePerLevel = getConfig("slowStrengthIncreasePerLevel", 0, Integer.class);
        headOffset = getConfig("headOffset", 1.2, Double.class);
    }
}
