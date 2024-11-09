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

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class ShockingStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    public List<ShockingStrikesData> data = new ArrayList<>();

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int slownessStrength;

    private int hitsNeeded;

    private double timeSpan;

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
                "Every hit <effect>Shock</effect>'s the target",
                "",
                EffectTypes.SHOCK.getDescription(0)
        };
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    public int getHitsNeeded(int level) {
        return hitsNeeded;
    }

    public double getTimeSpan(int level) {
        return timeSpan;
    }

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

            shockingData.addCount();
            shockingData.setLastHit(System.currentTimeMillis());
            event.addReason(getName());
            championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SHOCK, (long) (getDuration(level) * 1000L));
            if (shockingData.getCount() == getHitsNeeded(level)) {
                championsManager.getEffects().addEffect(event.getDamagee(), damager, EffectTypes.SLOWNESS, slownessStrength, (long) (getDuration(level) * 1000));
                data.remove(shockingData);
            }
        }
    }


    @UpdateEvent
    public void onUpdate() {
        data.removeIf(shockingData -> UtilTime.elapsed(shockingData.getLastHit(), (long) (getTimeSpan(0) * 1000L)));
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
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        hitsNeeded = getConfig("hitsNeeded", 3, Integer.class);
        timeSpan = getConfig("timeSpan", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        slownessStrength = getConfig("slownessStrength", 1, Integer.class);
    }
}
