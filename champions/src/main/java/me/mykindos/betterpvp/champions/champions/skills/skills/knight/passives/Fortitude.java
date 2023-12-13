package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Fortitude extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Double> health = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

    private double healRate;

    private double baseHeal;

    private double healIncreasePerLevel;

    @Inject
    public Fortitude(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Fortitude";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "After taking damage, you slowly",
                "regenerate up to <val>" + getMaxHeal(level) + "</val> health, at a",
                "rate of <stat>" + healRate + "</stat> health per second"
        };
    }

    public double getMaxHeal(int level) {
        return baseHeal + level * healIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player p)) return;
        int level = getLevel(p);
        if (level > 0) {
            //Student TODO: what is the divided by 2 for?
            health.put(p, Math.min(getMaxHeal(level), event.getDamage() / 2));
            last.put(p, System.currentTimeMillis());
        }
    }

    @UpdateEvent(delay = 250)
    public void update() {

        HashSet<Player> remove = new HashSet<>();
        for (Player cur : health.keySet()) {
            if (UtilTime.elapsed(last.get(cur), 1000L)) {
                health.put(cur, health.get(cur) - healRate);
                last.put(cur, System.currentTimeMillis());
                if (health.get(cur) <= 0) {
                    remove.add(cur);
                }
                UtilPlayer.health(cur, healIncreasePerLevel);
            }
        }

        for (Player cur : remove) {
            health.remove(cur);
            last.remove(cur);
        }
    }
    public void loadSkillConfig() {
        healRate = getConfig("healRate", 1.0, Double.class);
        baseHeal = getConfig("baseHeal", 3.0, Double.class);
        healIncreasePerLevel = getConfig("healIncreasePerLevel", 3.0, Double.class);
    }
}
