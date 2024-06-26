package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data.StackingHitData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Sharpshooter extends Skill implements PassiveSkill, DamageSkill {

    private final WeakHashMap<Player, StackingHitData> data = new WeakHashMap<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseMaxTimeBetweenShots;
    private double maxTimeBetweenShotsIncreasePerLevel;
    private int maxConsecutiveHits;
    private int maxConsecutiveHitsIncreasePerLevel;

    @Inject
    public Sharpshooter(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sharpshooter";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Each arrow hit will increase your",
                "damage by " + getValueString(this::getDamage, level) + " for " + getValueString(this::getMaxTimeBetweenShots, level) + " seconds",
                "",
                "Stacks up to " + getValueString(this::getMaxConsecutiveHits, level) + " times, and each",
                "hit sets duration to " + getValueString(this::getMaxTimeBetweenShots, level) + " seconds"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    public int getMaxConsecutiveHits(int level){
        return maxConsecutiveHits + ((level - 1) * maxConsecutiveHitsIncreasePerLevel);
    }

    public double getMaxTimeBetweenShots(int level) {
        return baseMaxTimeBetweenShots + ((level - 1) * maxTimeBetweenShotsIncreasePerLevel);
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
            event.setDamage(event.getDamage() + (Math.min(getMaxConsecutiveHits(level), hitData.getCharge()) * getDamage(level)));
            event.addReason("Sharpshooter");
            UtilMessage.simpleMessage(damager, getClassType().getName(), "<yellow>%d<gray> consecutive hits (<green>+%.2f damage<gray>)", hitData.getCharge(), (Math.min(getMaxConsecutiveHits(level), hitData.getCharge()) * getDamage(getLevel(damager))));
            damager.playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, (0.8f + (float)(hitData.getCharge() * 0.2)));
        }
    }

    @UpdateEvent(delay = 100)
    public void updateSharpshooterData() {
        data.entrySet().removeIf(entry -> {
            if (System.currentTimeMillis() > entry.getValue().getLastHit() + (long) (getMaxTimeBetweenShots(getLevel(entry.getKey())) * 1000L)) {
                Player player = entry.getKey();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.75f);
                UtilMessage.simpleMessage(player, getClassType().getName(), "<green>%s %d<gray> has ended at <yellow>%s<gray> damage", getName(), getLevel(player), (Math.min(getMaxConsecutiveHits(getLevel(player)), data.get(player).getCharge()) * getDamage(getLevel(player))));
                return true;
            }
            return false;
        });
    }


    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        baseMaxTimeBetweenShots = getConfig("baseMaxTimeBetweenShots", 5.0, Double.class);
        maxTimeBetweenShotsIncreasePerLevel = getConfig("maxTimeBetweenShotsIncreasePerLevel", 0.0, Double.class);
        maxConsecutiveHits = getConfig("maxConsecutiveHits", 2, Integer.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        maxConsecutiveHitsIncreasePerLevel = getConfig("maxConsecutiveHitsIncreasePerLevel", 2, Integer.class);
    }

}
