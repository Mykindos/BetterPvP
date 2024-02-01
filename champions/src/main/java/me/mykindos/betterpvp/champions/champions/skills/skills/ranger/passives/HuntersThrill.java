package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.StackingHitData;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HuntersThrill extends Skill implements PassiveSkill {

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
                "For each consecutive hit within <val>" + (baseMaxTimeBetweenShots + level),
                "seconds of each other, you gain",
                "increased movement speed up to a",
                "maximum of <effect>Speed " + UtilFormat.getRomanNumeral(maxConsecutiveHits + 1) + "</effect>",
        };
    }

    public double getMaxTimeBetweenShots(int level) {
        return baseMaxTimeBetweenShots + level * maxTimeBetweenShotsIncreasePerLevel;
    }

    public double getDuration(int level) {
        return baseDuration + level * durationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler
    public void onArrowHit(CustomDamageEvent event) {
        if(!(event.getProjectile() instanceof Arrow)) return;
        if(!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if(level > 0) {
            if(!data.containsKey(damager)) {
                data.put(damager, new StackingHitData());
            }

            StackingHitData hitData = data.get(damager);
            hitData.addCharge();
            damager.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getDuration(getLevel(damager)) * 20), Math.min(maxConsecutiveHits, hitData.getCharge()) - 1));
        }

    }


    @UpdateEvent(delay=100)
    public void updateHuntersThrillData() {
        data.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().getLastHit() + (long) ((getMaxTimeBetweenShots(getLevel(entry.getKey()))) * 1000L));
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig(){
        baseMaxTimeBetweenShots = getConfig("maxTimeBetweenShots", 8.0, Double.class);
        maxTimeBetweenShotsIncreasePerLevel = getConfig("maxTimeBetweenShotsIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("maxTimeBetweenShotsIncreasePerLevel", 6.0, Double.class);
        durationIncreasePerLevel = getConfig("maxTimeBetweenShotsIncreasePerLevel", 0.0, Double.class);


        maxConsecutiveHits = getConfig("maxConsecutiveHits", 4, Integer.class);
    }

}
