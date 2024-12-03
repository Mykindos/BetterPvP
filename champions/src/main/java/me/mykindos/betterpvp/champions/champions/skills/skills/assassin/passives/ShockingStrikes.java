package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.ShockingStrikesData;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.*;

@Singleton
@BPvPListener
public class ShockingStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    public List<ShockingStrikesData> data = new ArrayList<>();

    private double blindnessDuration; // Duration of blindness effect in seconds

    private double blindnessCooldown; // Cooldown time for applying blindness

    private double blindnessDurationIncreasePerLevel; // Additional duration per level

    private double blindnessCooldownDecreasePerLevel; // Cooldown reduction per level

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    private int hitsNeeded;

    private int hitsNeededBlind;

    private double timeSpan;

    private double timeSpanIncreasePerLevel;

    private static final long inactivityTime = 15000L;

    final private Map<UUID, Long> lastBlindnessTime = new HashMap<>();

    @Inject
    public ShockingStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Shocking Strikes";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hit a player " + getValueString(this::getHitsNeeded, level, 0) + " consecutive times without letting",
                getValueString(this::getTimeSpan, level) + " seconds pass to <effect>Slow</effect> them for " + getValueString(this::getDuration, level) + " seconds",
                "",
                "You <effect>Blind</effect> your opponents every " + getValueString(this::getHitsNeededBlind, level, 0) + " hits for " + getValueString(this::getBlindnessDuration, level) + " seconds",
                "You can apply blindness every " + getValueString(this::getBlindnessCooldown, level, 0) + " seconds",
                "",
                "Every hit <effect>Shock</effect>'s the target",
                "",
                EffectTypes.SHOCK.getDescription(0)
        };
    }


    public double getBlindnessDuration(int level) { return blindnessDuration + (blindnessDurationIncreasePerLevel * (level - 1)); }

    public int getHitsNeededBlind(int level) { return hitsNeededBlind; }

    public double getBlindnessCooldown(int level) { return  blindnessCooldown - (blindnessCooldownDecreasePerLevel * (level - 1)); }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    public int getHitsNeeded(int level) {
        return hitsNeeded;
    
    }

    public double getTimeSpan(int level) {
        return timeSpan + (timeSpanIncreasePerLevel * (level - 1)); }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;

        int level = getLevel(damager);
        if (level > 0) {
            ShockingStrikesData shockingData = getShockingStrikesData(damager, damagee);
            if (shockingData == null) {
                shockingData = new ShockingStrikesData(damager.getUniqueId(), damagee.getUniqueId());
                data.add(shockingData);
            }
            shockingData.addBlindCount();
            shockingData.addCount();
            shockingData.setLastHit(System.currentTimeMillis());
            event.addReason(getName());
            championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SHOCK, (long) (getDuration(level) * 1000L));

            long currentTime = System.currentTimeMillis();

            if (shockingData.getBlindCount() >= getHitsNeededBlind(level)) {
                long lastTime = lastBlindnessTime.getOrDefault(damager.getUniqueId(), 0L);
                if (currentTime - lastTime >= getBlindnessCooldown(level) * 1000L) {
                    championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.BLINDNESS, (long) (getBlindnessDuration(level) * 1000L));
                    lastBlindnessTime.put(damager.getUniqueId(), currentTime);
                    shockingData.resetBlindCount();

                }
            }

            if (shockingData.getCount() == getHitsNeeded(level)) {
                championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration(level) * 1000));
                shockingData.resetCount();
            }
        }
    }


    @UpdateEvent
    public void onUpdate() {
        data.removeIf(shockingData -> {
            if (UtilTime.elapsed(shockingData.getLastHit(), (long) (getTimeSpan(0) * 1000L))) {
                shockingData.resetCount();
            }
            return UtilTime.elapsed(shockingData.getLastHit(), inactivityTime);
        });
    }


    public ShockingStrikesData getShockingStrikesData(Player damager, Player damagee) {
        for (ShockingStrikesData shockingData : data) {
            if (shockingData.getPlayer().equals(damager.getUniqueId())) {
                if (shockingData.getTarget().equals(damagee.getUniqueId())) {
                    return shockingData;
                }
            }
        }
        return null;
    }

    @Override
    public void loadSkillConfig() {
        timeSpanIncreasePerLevel = getConfig("timeSpanIncreasePerLevel", 0.4, Double.class);
        hitsNeededBlind = getConfig("hitsNeededBlind", 3, Integer.class);
        blindnessDurationIncreasePerLevel = getConfig("blindnessDurationIncreasePerLevel", 0.0, Double.class);
        blindnessCooldownDecreasePerLevel = getConfig("blindnessCooldownDecreasePerLevel", 0.5, Double.class);
        blindnessDuration = getConfig("blindnessDuration", 2.5, Double.class);
        blindnessCooldown = getConfig("blindnessCooldown", 5.0, Double.class);
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        hitsNeeded = getConfig("hitsNeeded", 2, Integer.class);
        timeSpan = getConfig("timeSpan", 0.8, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }
}
