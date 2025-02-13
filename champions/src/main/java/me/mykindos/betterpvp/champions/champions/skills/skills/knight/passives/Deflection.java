package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
    @Getter
    private double timeBetweenCharges;
    @Getter
    private double timeOutOfCombat;
    @Getter
    private int maxCharges;
    private double damageReduction;

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
    public String[] getDescription() {
        return new String[]{
                "You gain <stat>1</stat> charge every <val>" + getTimeBetweenCharges() + "</val> seconds.",
                "You can store a maximum of <val>" + getMaxCharges() + "</val> charges",
                "",
                "When attacked, the damage you take is",
                "reduced by <val>" + getDamageReductionPerCharge() + "</val> damage per charge",
        };
    }

    public double getDamageReductionPerCharge() {
        return damageReduction;
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

        if (hasSkill(player)) {
            int charge = charges.remove(player.getUniqueId());
            event.setDamage(event.getDamage() - charge);

        }
    }

    @UpdateEvent(delay = 250)
    public void addCharge() {

        for (Player cur : Bukkit.getOnlinePlayers()) {
            if (hasSkill(cur)) {
                if (charges.containsKey(cur.getUniqueId())) {
                    Gamer gamer = championsManager.getClientManager().search().online(cur).getGamer();
                    if (UtilTime.elapsed(gamer.getLastDamaged(), (long) getTimeOutOfCombat() * 1000)) {
                        if (!championsManager.getCooldowns().use(cur, getName(), getTimeBetweenCharges(), false))
                            return;
                        int charge = charges.get(cur.getUniqueId());
                        if (charge < getMaxCharges()) {
                            charge = Math.min(getMaxCharges(), charge + 1);
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
        maxCharges = getConfig("maxCharges", 1, Integer.class);
        damageReduction = getConfig("damageReduction", 1.0, Double.class);
    }

}
