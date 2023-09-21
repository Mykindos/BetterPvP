package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import java.util.HashSet;
import java.util.WeakHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class RepeatedStrikes extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Integer> repeat = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

    @Inject
    public RepeatedStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Repeated Strikes";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Each time you attack, your damage",
                "increases by 1",
                "You can get up to <val>" + level + "</val> bonus damage.",
                "",
                "Not attacking for 2 seconds clears",
                "your bonus damage."};
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!championsManager.getRoles().hasRole(damager, Role.ASSASSIN)) return;

        int level = getLevel(damager);
        if (level > 0) {

            if (!repeat.containsKey(damager)) {
                repeat.put(damager, 0);
            }
            event.setDamage(event.getDamage() + repeat.get(damager));
            repeat.put(damager, Math.min(level, repeat.get(damager) + 1));
            last.put(damager, System.currentTimeMillis());
            event.setReason(getName());

        }
    }


    @UpdateEvent(delay = 500)
    public void onUpdate() {

        HashSet<Player> remove = new HashSet<>();

        for (Player player : repeat.keySet()) {
            if (UtilTime.elapsed(last.get(player), 2000)) {
                remove.add(player);
            }
        }

        for (Player player : remove) {
            repeat.remove(player);
            last.remove(player);
        }
    }

}
