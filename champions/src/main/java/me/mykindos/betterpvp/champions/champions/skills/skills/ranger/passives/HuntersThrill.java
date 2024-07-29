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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HuntersThrill extends Skill implements PassiveSkill, MovementSkill, BuffSkill {

    private double speedDuration;
    private int maxSpeedLevel;
    private int maxSpeedLevelIncreasePerLevel;
    private double speedDurationIncreasePerLevel;
    private final Map<Player, Integer> speedLevels = new WeakHashMap<>();
    private final Map<Player, Long> lastHitTime = new WeakHashMap<>();

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
                "Every melee hit you land will increase your speed",
                "by one speed level up to a maximum of <effect>Speed " + UtilFormat.getRomanNumeral(getMaxSpeedLevel(level)) + "</effect>",
                "",
                "Not hitting a target for " + getValueString(this::getSpeedDuration, level) + " seconds",
                "will lower your speed by one level",
        };
    }

    public double getSpeedDuration(int level) {
        return speedDuration + ((level - 1) * speedDurationIncreasePerLevel);
    }

    public int getMaxSpeedLevel(int level) {
        return maxSpeedLevel + ((level - 1) * maxSpeedLevelIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        int level = getLevel(damager);
        if (level > 0) {
            System.out.println("test");
            int currentSpeedLevel = speedLevels.getOrDefault(damager, 0);
            System.out.println("current speed: " + currentSpeedLevel);
            int maxSpeed = getMaxSpeedLevel(level);
            if (currentSpeedLevel < maxSpeed) {
                currentSpeedLevel++;
                System.out.println("current speed2: " + currentSpeedLevel);
                speedLevels.put(damager, currentSpeedLevel);
                lastHitTime.put(damager, System.currentTimeMillis());
            }

            championsManager.getEffects().addEffect(damager, damager, EffectTypes.SPEED, currentSpeedLevel, (long) (getSpeedDuration(level) * 1000));
        }
    }

    @UpdateEvent
    public void updateHuntersThrillData() {
        long currentTime = System.currentTimeMillis();
        for (Player player : speedLevels.keySet()) {
            int level = getLevel(player);
            if (level > 0) {
                long lastHit = lastHitTime.getOrDefault(player, 0L);
                double duration = getSpeedDuration(level) * 1000;
                if (currentTime - lastHit > duration) {
                    speedLevels.put(player, 0);
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
        speedDuration = getConfig("speedDuration", 1.5, Double.class);
        maxSpeedLevel = getConfig("maxSpeedLevel", 2, Integer.class);
        maxSpeedLevelIncreasePerLevel = getConfig("maxSpeedLevelIncreasePerLevel", 1, Integer.class);
        speedDurationIncreasePerLevel = getConfig("speedDurationIncreasePerLevel", 0.0, Double.class);
    }

}
