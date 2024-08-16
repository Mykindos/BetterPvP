package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ComboAttack extends Skill implements PassiveSkill, Listener, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, Map<Player, Double>> repeat = new WeakHashMap<>();
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
                "damage increases by " + getValueString(this::getDamageIncrement, level),
                "",
                "You can deal up to " + getValueString(this::getMaxDamageIncrement, level) + " bonus damage",
                "",
                "Not attacking for " + getValueString(this::getDuration, level) + " seconds",
                "will reset your bonus damage"};
    }

    public double getMaxDamageIncrement(int level) {
        return baseDamageIncrement + (level - 1) * damageIncrement;
    }

    private double getDamageIncrement(int level) {
        return damageIncrement;
    }

    private double getDuration(int level) {
        return duration;
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
        if (!(event.getDamagee() instanceof Player target)) return;
        if (!championsManager.getRoles().hasRole(damager, Role.ASSASSIN)) return;

        int level = getLevel(damager);
        if (level > 0) {
            repeat.putIfAbsent(damager, new HashMap<>());
            Map<Player, Double> targetDamage = repeat.get(damager);
            targetDamage.putIfAbsent(target, 0.0);

            double cur = targetDamage.get(target);
            event.setDamage(event.getDamage() + cur);
            targetDamage.put(target, Math.min(getMaxDamageIncrement(level), cur + damageIncrement));
            last.put(damager, System.currentTimeMillis());
            event.addReason(getName());

            damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.7f + (0.3f * targetDamage.get(target))));
        }
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {

        HashSet<Player> remove = new HashSet<>();

        for (Player player : last.keySet()) {
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