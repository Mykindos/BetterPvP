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
import org.bukkit.ChatColor;
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
public class Archery extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Integer> repeat = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> last = new WeakHashMap<>();

    @Inject
    public Archery(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Archery";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your arrows deal " + 2 + (level),
                " extra damage"};
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

//code in actual skill here
//make sure to define level
