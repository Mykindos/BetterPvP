package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.SilencingStrikesData;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class SilencingStrikes extends Skill implements PassiveSkill, Listener, DebuffSkill, OffensiveSkill {

    public List<SilencingStrikesData> data = new ArrayList<>();

    @Inject
    public SilencingStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    private int hitsNeeded;
    private double timeSpan;

    private double baseDuration;

    private double durationIncreasePerLevel;

    @Override
    public String getName() {
        return "Silencing Strikes";
    }

    @Override
    public Component[] getDescription(int level) {
        Component hits = getValueComponent(this::getHitsNeeded, level, 0);
        Component time = getValueComponent(this::getTimeSpan, level);
        Component duration = getValueComponent(this::getDuration, level);
        Component silence = Translations.component("champions.skill.effect.silence.name").color(NamedTextColor.WHITE);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.silencing-strikes.description",
                hits,
                time,
                duration,
                silence
        );
        Component silenceDetail = Translations.component("champions.skill.effect.silence.name").color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines("champions.skill.effect.silence.detail", silenceDetail);
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
    public void onDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;

        int level = getLevel(damager);
        if (level > 0) {
            SilencingStrikesData silenceData = getSilencingStrikesData(damager, damagee);
            if (silenceData == null) {
                silenceData = new SilencingStrikesData(damager.getUniqueId(), damagee.getUniqueId());
                data.add(silenceData);
            }

            silenceData.addCount();
            silenceData.setLastHit(System.currentTimeMillis());
            event.addReason(getName());
            if (silenceData.getCount() == getHitsNeeded(level)) {
                championsManager.getEffects().addEffect(damagee, EffectTypes.SILENCE, (long) ((getDuration(level) * 1000L)));
                data.remove(silenceData);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        data.removeIf(silenceData -> UtilTime.elapsed(silenceData.getLastHit(), (long) (getTimeSpan(0) * 1000L)));
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
        baseDuration = getConfig("baseDuration", 0.8, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.4, Double.class);
    }
}
