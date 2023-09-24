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
public class SiphoningStrikes extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Double> repeat = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

    @Inject
    public SiphoningStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Siphoning Strikes";
    }

    private double healthIncrement;
    private double duration;

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Each time you attack, you siphon health",
                "from the enemy, starting at <stat>" + ((healthIncrement * 100) * 2) + "%</stat>",
                "and increasing by <val>" + (healthIncrement * 100) + "%</val> for each hit",
                "",
                "You can gain up to <val>" + (20+(level * (healthIncrement * 100))) + "%</val> health per hit",
                "",
                "Not attacking for <stat>" + duration + "</stat> seconds resets",
                "your bonus health back to <stat>" +((healthIncrement * 100) * 2)+"%"};
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
        if (!(event.getDamagee() instanceof Player)) return;

        int level = getLevel(damager);
        if (level > 0) {
            double currentIncrement = repeat.getOrDefault(damager, 2 * healthIncrement);

            currentIncrement = Math.min(currentIncrement + healthIncrement, (level * healthIncrement) + (2 * healthIncrement));

            double healthToAdd = event.getDamage() * currentIncrement;
            damager.setHealth(Math.min(damager.getHealth() + healthToAdd, damager.getMaxHealth()));

            repeat.put(damager, currentIncrement);
            last.put(damager, System.currentTimeMillis());
        }
    }


    @UpdateEvent(delay = 500)
    public void onUpdate() {

        HashSet<Player> remove = new HashSet<>();

        for (Player player : repeat.keySet()) {
            if (UtilTime.elapsed(last.get(player), (long) duration * 1000)) {
                remove.add(player);
            }
        }

        for (Player player : remove) {
            repeat.remove(player);
            last.remove(player);
        }
    }
    @Override
    public void loadSkillConfig(){
        healthIncrement = getConfig("healthIncrement", 0.1, Double.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
