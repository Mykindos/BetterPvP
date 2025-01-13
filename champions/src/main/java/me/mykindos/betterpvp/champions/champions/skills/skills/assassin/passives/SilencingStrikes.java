package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.SilencingStrikesData;
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
public class SilencingStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    public List<SilencingStrikesData> data = new ArrayList<>();
    @Getter
    private int hitsNeeded;
    @Getter
    private double timeSpan;
    @Getter
    private double duration;

    @Inject
    public SilencingStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Silencing Strikes";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Hit a player <val>" + getHitsNeeded() + " consecutive times without letting",
                getTimeSpan() + " seconds pass to <effect>Silence</effect> them for <val>" + getDuration() + "</val> seconds",
                "",
                EffectTypes.SILENCE.getDescription(0)
        };
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

        if (!hasSkill(damager)) return;

        SilencingStrikesData silenceData = getSilencingStrikesData(damager, damagee);
        if (silenceData == null) {
            silenceData = new SilencingStrikesData(damager.getUniqueId(), damagee.getUniqueId());
            data.add(silenceData);


            silenceData.addCount();
            silenceData.setLastHit(System.currentTimeMillis());
            event.addReason(getName());
            if (silenceData.getCount() == getHitsNeeded()) {
                championsManager.getEffects().addEffect(damagee, EffectTypes.SILENCE, (long) (getDuration() * 1000L));
                data.remove(silenceData);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        data.removeIf(silenceData -> UtilTime.elapsed(silenceData.getLastHit(), (long) (getTimeSpan() * 1000L)));
    }

    public SilencingStrikesData getSilencingStrikesData(Player damager, Player damagee) {
        for (SilencingStrikesData silenceData : data) {
            if (silenceData.getPlayer().equals(damager.getUniqueId())) {
                if (silenceData.getTarget().equals(damagee.getUniqueId())) {
                    return silenceData;
                }
            }
        }
        return null;
    }

    public void loadSkillConfig() {
        hitsNeeded = getConfig("hitsNeeded", 3, Integer.class);
        timeSpan = getConfig("timeSpan", 0.8, Double.class);
        duration = getConfig("duration", 0.8, Double.class);
    }
}
