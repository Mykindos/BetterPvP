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
                "Each time you attack, you heal ",
                "<val>" + ((healthIncrement * 100) + (10 * (level - 1))) + "%</val> of the damage you did",
                "",
                "Ignores armor"
        };
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
            damager.setHealth(damager.getHealth()+(event.getDamage() * (healthIncrement + (0.1 * (level - 1)))));
        }
    }
    @Override
    public void loadSkillConfig(){
        healthIncrement = getConfig("healthIncrement", 0.20, Double.class);
    }
}
