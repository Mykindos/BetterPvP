package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.WeakHashMap;
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

@Singleton
@BPvPListener
public class Fortitude extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Integer> health = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

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
                "regenerate up to <val>" + (3 + (level - 1)) + "</val> health, at a",
                "rate of 1 health per 1 seconds.",
                "",
                "This does not stack, and is reset",
                "if you are hit again."};
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
            health.put(p, Math.min((2 + level), (int) event.getDamage() / 2));
            last.put(p, System.currentTimeMillis());
        }

    }

    @UpdateEvent(delay = 250)
    public void update() {

        HashSet<Player> remove = new HashSet<>();
        for (Player cur : health.keySet()) {
            if (UtilTime.elapsed(last.get(cur), 2500L)) {
                health.put(cur, health.get(cur) - 1);
                last.put(cur, System.currentTimeMillis());
                if (health.get(cur) <= 0) {
                    remove.add(cur);
                }
                UtilPlayer.health(cur, 1.0D);
            }
        }

        for (Player cur : remove) {
            health.remove(cur);
            last.remove(cur);
        }
    }

}
