package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class VitalitySpores extends Skill implements PassiveSkill, DefensiveSkill, HealthSkill {

    private final WeakHashMap<Player, Long> lastDamagedMap;

    private double baseDuration;

    private double durationDecreasePerLevel;

    private int regenerationStrength;

    @Inject
    public VitalitySpores(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        this.lastDamagedMap = new WeakHashMap<>();
    }

    @Override
    public String getName() {
        return "Vitality Spores";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "After " + getValueString(this::getDuration, level) + " seconds of not taking damage,",
                "forest spores surround you, giving",
                "you <effect>Regeneration " + UtilFormat.getRomanNumeral(regenerationStrength) + "</effect>",
                "",
                "You will keep the buff until you take damage"
        };
    }

    public double getDuration(int level) {
        return baseDuration - ((level - 1) * durationDecreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int level = getLevel(player);
            if (level > 0) {
                long lastDamaged = lastDamagedMap.getOrDefault(player, 0L);
                if (UtilTime.elapsed(lastDamaged, (long) (getDuration(level) * 1000L))) {
                    championsManager.getEffects().addEffect(player, EffectTypes.REGENERATION, getName(), regenerationStrength, 1000);
                }
            }
        }

    }

    @EventHandler
    public void onDamageReceived(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        if (hasSkill(player)) {
            championsManager.getEffects().removeEffect(player, EffectTypes.REGENERATION, getName());

            lastDamagedMap.put(player, System.currentTimeMillis());
        }
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 7.0, Double.class);
        durationDecreasePerLevel = getConfig("durationDecreasePerLevel", 1.0, Double.class);

        regenerationStrength = getConfig("regenerationStrength", 1, Integer.class);
    }

}
