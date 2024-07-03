package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.StackingHitData;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HuntersThrill extends Skill implements PassiveSkill, MovementSkill, BuffSkill {

    private final WeakHashMap<Player, StackingHitData> data = new WeakHashMap<>();

    private double baseMaxTimeBetweenShots;

    private double maxTimeBetweenShotsIncreasePerLevel;

    private double baseDuration;

    private double durationIncreasePerLevel;

    private int maxConsecutiveHits;


    @Inject
    public HuntersThrill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hunters Thrill";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For each consecutive hit within " + getValueString(this::getMaxTimeBetweenShots, level),
                "seconds of each other, you gain",
                "increased movement speed up to a",
                "maximum of <effect>Speed " + UtilFormat.getRomanNumeral(maxConsecutiveHits) + "</effect>"
        };
    }

    public double getMaxTimeBetweenShots(int level) {
        return baseMaxTimeBetweenShots + ((level - 1) * maxTimeBetweenShotsIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            if (!data.containsKey(damager)) {
                data.put(damager, new StackingHitData());
            }

            StackingHitData hitData = data.get(damager);
            hitData.addCharge();
            championsManager.getEffects().addEffect(damager, EffectTypes.SPEED, Math.min(maxConsecutiveHits, hitData.getCharge()), (long) (getDuration(level) * 1000));
        }

    }


    @UpdateEvent(delay = 100)
    public void updateHuntersThrillData() {
        data.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().getLastHit() + (long) ((getMaxTimeBetweenShots(getLevel(entry.getKey()))) * 1000L));
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxTimeBetweenShots = getConfig("baseMaxTimeBetweenShots", 8.0, Double.class);
        maxTimeBetweenShotsIncreasePerLevel = getConfig("maxTimeBetweenShotsIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 6.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        maxConsecutiveHits = getConfig("maxConsecutiveHits", 4, Integer.class);
    }

}
