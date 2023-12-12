package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
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
public class Deflection extends Skill implements PassiveSkill {


    private double timeBetweenCharges;
    private double timeOutOfCombat;

    private int baseCharges;

    private final WeakHashMap<Player, Integer> charges = new WeakHashMap<>();

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
                "You gain <stat>1</stat> charge every <stat>" + timeBetweenCharges + "</stat> seconds.",
                "You can store a maximum of <val>" + (level) + "</val> charges",
                "",
                "When attacked, the damage you take is",
                "reduced by the number of deflection charges",
        };
    }

    public int getMaxCharges(int level) {
        return baseCharges + level;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!charges.containsKey(player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int charge = charges.get(player);
            event.setDamage(event.getDamage() - charge);
            charges.remove(player);
        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            int level = getLevel(cur);
            if (level > 0) {
                if (charges.containsKey(cur)) {
                    Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
                        if (UtilTime.elapsed(gamer.getLastDamaged(), (long) timeOutOfCombat * 1000)) {
                            if (!championsManager.getCooldowns().use(cur, getName(), timeBetweenCharges, false)) return;
                            int charge = charges.get(cur);
                            if (charge < getMaxCharges(level)) {
                                charge = Math.min(getMaxCharges(level), charge + 1);
                                UtilMessage.simpleMessage(cur, getClassType().getName(), "Deflection charge: <yellow>%d", charge);
                                charges.put(cur, charge);
                            }
                        }
                    }

                } else {
                charges.put(cur, 0);
            }
        }

    }

    @Override
    public void loadSkillConfig() {
        timeBetweenCharges = getConfig("timeBetweenCharges", 2.0, Double.class);
        timeOutOfCombat = getConfig("timeOutOfCombat", 2.0, Double.class);
        baseCharges = getConfig("baseCharges", 0, Integer.class);
    }

}
