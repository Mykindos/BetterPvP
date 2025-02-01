package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HuntersThrill extends Skill implements PassiveSkill, MovementSkill, BuffSkill {

    private final WeakHashMap<Player, StackingHitData> data = new WeakHashMap<>();

    @Getter
    private double maxTimeBetweenShots;
    @Getter
    private double duration;
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
    public String[] getDescription() {
        return new String[]{
                "For each consecutive hit within <val>" + getMaxTimeBetweenShots(),
                "seconds of each other, you gain",
                "increased movement speed for <val>" + getDuration() + "</val> seconds",
                "up to a maximum of <effect>Speed " + UtilFormat.getRomanNumeral(maxConsecutiveHits) + "</effect>"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowHit(CustomDamageEvent event) {
        Projectile projectile = event.getProjectile();
        boolean isArrow = projectile instanceof Arrow;
        boolean isTrident = projectile instanceof Trident;
        if (!(isArrow) && !(isTrident)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (hasSkill(damager)) {
            if (!data.containsKey(damager)) {
                data.put(damager, new StackingHitData());
            }

            StackingHitData hitData = data.get(damager);
            hitData.addCharge();
            championsManager.getEffects().addEffect(damager, EffectTypes.SPEED, Math.min(maxConsecutiveHits, hitData.getCharge()), (long) (getDuration() * 1000));
        }

    }


    @UpdateEvent(delay = 100)
    public void updateHuntersThrillData() {
        data.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue().getLastHit() + (long) (getMaxTimeBetweenShots() * 1000L));
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        maxTimeBetweenShots = getConfig("maxTimeBetweenShots", 8.0, Double.class);
        duration = getConfig("duration", 6.0, Double.class);
        maxConsecutiveHits = getConfig("maxConsecutiveHits", 4, Integer.class);
    }

}
