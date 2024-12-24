package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.ComboAttackData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Iterator;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ComboAttack extends Skill implements PassiveSkill, Listener, DamageSkill, OffensiveSkill {

    private final WeakHashMap<Player, ComboAttackData> repeat = new WeakHashMap<>();

    private double baseDamageIncrement;
    private double damageIncrement;
    private double duration;
    private double durationIncreasePerLevel;


    @Inject
    public ComboAttack(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Combo Attack";
    }


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
        return duration + (level - 1) * durationIncreasePerLevel;
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
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!championsManager.getRoles().hasRole(damager, Role.ASSASSIN)) return;

        int level = getLevel(damager);
        if (level > 0) {

            if (!(repeat.containsKey(damager))) {
                repeat.put(damager, new ComboAttackData(event.getDamagee().getUniqueId(), baseDamageIncrement, System.currentTimeMillis()));
            } else if (repeat.get(damager).getLastTarget() != event.getDamagee().getUniqueId()) {
                repeat.remove(damager);
                return;
            }

            double cur = repeat.get(damager).getDamageIncrement();
            event.setDamage(event.getDamage() + cur);

            repeat.get(damager).setDamageIncrement(Math.min(cur + damageIncrement, getMaxDamageIncrement(level)));
            repeat.get(damager).setLastTarget(event.getDamagee().getUniqueId());
            repeat.get(damager).setLast(System.currentTimeMillis());

            event.addReason(getName());

            damager.getWorld().playSound(damager.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, (float) (0.7f + (0.3f * repeat.get(damager).getDamageIncrement())));

        }
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        Iterator<Player> iterator = repeat.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            int level = getLevel(player);
            if (UtilTime.elapsed(repeat.get(player).getLast(), (long) getDuration(level) * 1000)) {
                UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %d</green> has ended.", getName(), level));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 5.0f);
                iterator.remove();
            }
        }
    }


    @Override
    public void loadSkillConfig() {
        baseDamageIncrement = getConfig("baseDamageIncrement", 1.0, Double.class);
        damageIncrement = getConfig("damageIncrement", 1.0, Double.class);
        duration = getConfig("duration", 0.8, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.4, Double.class);
    }
}
