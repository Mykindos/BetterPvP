package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
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

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class Deflection extends Skill implements PassiveSkill, DefensiveSkill {


    private double timeBetweenCharges;
    private double timeOutOfCombat;

    private int baseCharges;

    private final HashMap<UUID, Integer> charges = new HashMap<>();

    @Inject
    public Deflection(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Deflection";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You gain <stat>1</stat> charge every " + getValueString(this::getTimeBetweenCharges, level) + " seconds.",
                "You can store a maximum of " + getValueString(this::getMaxCharges, level, 0) + " charges",
                "",
                "When attacked, the damage you take is",
                "reduced by the number of deflection charges",
        };
    }

    public int getMaxCharges(int level) {
        return baseCharges + (level - 1);
    }

    public double getTimeBetweenCharges(int level) {
        return timeBetweenCharges;
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
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!charges.containsKey(player.getUniqueId())) return;

        int level = getLevel(player);
        if (level > 0) {
            int charge = charges.remove(player.getUniqueId());
            event.setDamage(event.getDamage() - charge);

        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            int level = getLevel(cur);
            if (level > 0) {
                if (charges.containsKey(cur.getUniqueId())) {
                    Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
                    if (UtilTime.elapsed(gamer.getLastDamaged(), (long) timeOutOfCombat * 1000)) {
                        if (!championsManager.getCooldowns().use(cur, getName(), timeBetweenCharges, false)) return;
                        int charge = charges.get(cur.getUniqueId());
                        if (charge < getMaxCharges(level)) {
                            charge = Math.min(getMaxCharges(level), charge + 1);
                            UtilMessage.simpleMessage(cur, getClassType().getName(), "Deflection charge: <yellow>%d", charge);
                            charges.put(cur.getUniqueId(), charge);
                        }
                    }
                } else {
                    charges.put(cur.getUniqueId(), 0);
                }
            } else {
                charges.remove(cur.getUniqueId());
            }
        }

    }

    @Override
    public void loadSkillConfig() {
        timeBetweenCharges = getConfig("timeBetweenCharges", 2.0, Double.class);
        timeOutOfCombat = getConfig("timeOutOfCombat", 2.0, Double.class);
        baseCharges = getConfig("baseCharges", 1, Integer.class);
    }

}
