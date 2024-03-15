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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
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

    private double baseDamageIncrement;
    private double damageIncrement;
    private double duration;

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Each time you attack, your",
                "damage increases by <stat>" + damageIncrement + "</stat>",
                "",
                "You can deal up to <val>" + getMaxDamageIncrement(level) + "</val> bonus damage",
                "",
                "Not attacking for <stat>" + duration + "</stat> seconds",
                "will reset your bonus damage"};
    }

    public double getMaxDamageIncrement(int level) {
        return baseDamageIncrement + (level - 1) * damageIncrement;
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!championsManager.getRoles().hasRole(damager, Role.ASSASSIN)) return;

        int level = getLevel(damager);
        if (level > 0) {

            if (!repeat.containsKey(damager)) {
                repeat.put(damager, 0.0);
            }
            double cur = repeat.get(damager);
            event.setDamage(event.getDamage() + cur);
            repeat.put(damager, Math.min(getMaxDamageIncrement(level), cur + damageIncrement));
            last.put(damager, System.currentTimeMillis());
            event.addReason(getName());

            damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.7f + (0.3f * repeat.get(damager))));

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
    public void loadSkillConfig() {
        baseDamageIncrement = getConfig("baseDamageIncrement", 1.0, Double.class);
        damageIncrement = getConfig("damageIncrement", 1.0, Double.class);
        duration = getConfig("duration", 2.0, Double.class);
    }
}
