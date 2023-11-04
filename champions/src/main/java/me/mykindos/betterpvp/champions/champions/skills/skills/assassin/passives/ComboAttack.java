package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ComboAttack extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Double> repeat = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

    @Inject
    public ComboAttack(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Combo Attack";
    }

    private double damageIncrement;
    private double duration;

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Each time you attack, your",
                "damage increases by <stat>" + damageIncrement + "</stat>",
                "",
                "You can deal up to <val>" + (level * damageIncrement) + "</val> bonus damage",
                "",
                "Not attacking for <stat>" + duration + "</stat> seconds",
                "will reset your bonus damage"};
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!championsManager.getRoles().getRole(damager).hasSkill(getType(), getName())) return;

        int level = getLevel(damager);
        if (level > 0) {

            if (!repeat.containsKey(damager)) {
                repeat.put(damager, 0.0);
            }
            event.setDamage(event.getDamage() + repeat.get(damager));
            repeat.put(damager, Math.min((level * damageIncrement), repeat.get(damager) + damageIncrement));
            last.put(damager, System.currentTimeMillis());
            event.addReason(getName());

        }
    }

    @Override
    public String getDefaultClassString() {
        return "assassin";
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
        damageIncrement = getConfig("damageIncrement", 1.0, Double.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
