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
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

@BPvPListener
@Singleton
public class Thorns extends Skill implements PassiveSkill, Listener {

    private final Map<LivingEntity, Queue<Long>> hitTimestamps = new HashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;
    private double reflectTime;
    private int  hitsToTrigger;

    @Inject
    public Thorns(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Thorns";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "If you are hit <stat>" + hitsToTrigger + "</stat> times within <stat>" + getReflectTime(level) +"</stat> seconds",
                "you will reflect back <val>" + getDamage(level) + "%</val> damage",
        };
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getReflectTime(int level){
        return reflectTime;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (event.getDamager() == null) return;

        if(getLevel(player) < 1) return;

        LivingEntity damager = event.getDamager();
        hitTimestamps.putIfAbsent(player, new LinkedList<>());
        Queue<Long> timestamps = hitTimestamps.get(player);

        timestamps.offer(System.currentTimeMillis());

        while (!timestamps.isEmpty() && System.currentTimeMillis() - timestamps.peek() > (reflectTime * 1000)) {
            timestamps.poll();
        }

        if (timestamps.size() == hitsToTrigger) {
            player.getWorld().playSound(player.getLocation(), Sound.ENCHANT_THORNS_HIT, 1.0F, 1.0F);
            UtilDamage.doCustomDamage(new CustomDamageEvent(damager, player, null, DamageCause.ENTITY_ATTACK, getDamage(getLevel(player)), true, getName()));
            timestamps.clear();
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        reflectTime = getConfig("reflectTime", 2.0, Double.class);
        hitsToTrigger = getConfig("hitsToTrigger", 3, Integer.class);
    }
}
