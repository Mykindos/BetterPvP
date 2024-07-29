package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
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

    private final Map<UUID, Location> hitLocations = new HashMap<>();
    private final Map<UUID, Location> headLocations = new HashMap<>();
    private final Map<UUID, Location> footLocations = new HashMap<>();


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
                "Melee hits to the head deal " + getValueString(this::getDamage, level) + " more damage and",
                "melee hits to the feet give <effect>Slowness " + UtilFormat.getRomanNumeral(getSlowStrength(level)) + "</effect> for " + getValueString(this::getSlowDuration, level) + " seconds"
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
            Entity damagee = event.getDamagee();
            int level = getLevel(damager);
            headLocations.remove(damager.getUniqueId());
            hitLocations.remove(damager.getUniqueId());
            footLocations.remove(damager.getUniqueId());
            if (level > 0) {
                RayTraceResult result = damagee.getWorld().rayTraceEntities(
                        damager.getEyeLocation(),
                        damager.getEyeLocation().getDirection(),
                        5.0,
                        hitboxSize,
                        entity -> entity instanceof LivingEntity && !entity.equals(damager)
                );

                if (result != null && result.getHitEntity() instanceof LivingEntity hitEntity) {
                    Location hitPos = result.getHitPosition().toLocation(damager.getEyeLocation().getWorld());
                    hitPos.setX(hitEntity.getLocation().getX());
                    hitPos.setZ(hitEntity.getLocation().getZ());
                    Location headPos = hitEntity.getEyeLocation().add(0, headOffset, 0);
                    Location footPos = hitEntity.getLocation();

                    double headDistance = headPos.distance(hitPos);
                    double footDistance = footPos.distance(hitPos);

                    if (headDistance <= footDistance) {
                        headLocations.put(damager.getUniqueId(), headPos);
                        event.setDamage(event.getDamage() + getDamage(level));
                        damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.5f, 2.0f);
                        damager.getWorld().playEffect(event.getDamagee().getEyeLocation(), Effect.STEP_SOUND, Material.REDSTONE_WIRE);
                        event.addReason("Decapitation Tactics");
                    } else {
                        footLocations.put(damager.getUniqueId(), footPos);
                        championsManager.getEffects().addEffect(hitEntity, damager, EffectTypes.SLOWNESS, getSlowStrength(level), (long) (getSlowDuration(level) * 1000));
                        damagee.getWorld().playSound(damagee.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.5f, 2.0f);
                        damager.getWorld().playEffect(event.getDamagee().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_WIRE);
                        event.addReason("Slowness Tactics");
                    }
                    hitLocations.put(damager.getUniqueId(), hitPos);
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
        damage = getConfig("percent", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("percentIncreasePerLevel", 0.25, Double.class);
        hitboxSize = getConfig("hitboxSize", 0.5, Double.class);
        slowDuration = getConfig("slowDuration", 0.5, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.25, Double.class);
        slowStrength = getConfig("slowStrength", 1, Integer.class);
        slowStrengthIncreasePerLevel = getConfig("slowStrengthIncreasePerLevel", 0, Integer.class);
        headOffset = getConfig("headOffset", 0.8, Double.class);
    }
}
