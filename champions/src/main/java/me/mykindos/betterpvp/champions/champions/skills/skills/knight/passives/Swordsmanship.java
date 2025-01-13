package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Swordsmanship extends Skill implements PassiveSkill, OffensiveSkill, DamageSkill {

    @Getter
    private double timeBetweenCharges;
    private double timeOutOfCombat;
    @Getter
    private int maxCharges;
    private double damagePerCharge;
    private final WeakHashMap<Player, Integer> charges = new WeakHashMap<>();

    @Inject
    public Swordsmanship(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Swordsmanship";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "You gain 1 charge every <val>" + getTimeBetweenCharges() + "</val> seconds,",
                "storing up to a maximum of <val>" + getMaxCharges() + "</val> charges",
                "",
                "When you attack, your damage is increased",
                "by <stat>" + getDamage(1) + "</stat> for each charge you have",
                "",
                "This only applies to swords"
        };
    }

    public double getDamage(int charge) {
        return (damagePerCharge) * charge;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!charges.containsKey(player)) return;
        if (!SkillWeapons.isHolding(player, SkillType.SWORD)) return;

        if (hasSkill(player)) {
            int charge = charges.get(player);
            event.setDamage(event.getDamage() + getDamage(charge));
            charges.remove(player);
        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            if (hasSkill(cur)) {
                if (charges.containsKey(cur)) {
                    Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
                    if (UtilTime.elapsed(gamer.getLastDamaged(), (long) timeOutOfCombat * 1000)) {
                        if (!championsManager.getCooldowns().use(cur, getName(), timeBetweenCharges, false)) return;
                        int charge = charges.get(cur);
                        final int max = getMaxCharges();
                        if (charge < max) {
                            charge = Math.min(max, charge + 1);
                            UtilMessage.simpleMessage(cur, getClassType().getName(), "Swordsmanship charge: <yellow>%d", charge);
                            charges.put(cur, charge);
                        }
                    }
                } else {
                    charges.put(cur, 0);
                }
            }
        }

    }

    @Override
    public void loadSkillConfig() {
        timeBetweenCharges = getConfig("timeBetweenCharges", 2.0, Double.class);
        timeOutOfCombat = getConfig("timeOutOfCombat", 2.5, Double.class);
        damagePerCharge = getConfig("damagePerCharge", 1.0, Double.class);
        maxCharges = getConfig("maxCharges", 3, Integer.class);
    }

}


