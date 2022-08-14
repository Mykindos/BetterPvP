package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Resistance extends Skill implements PassiveSkill {

    @Inject
    public Resistance(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Resistance";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You take " + ChatColor.GREEN + (level * 15) + ChatColor.GRAY + "% less damage",
                "but you deal " + ChatColor.GREEN + (level * 15) + ChatColor.GRAY + "% less as well"
        };
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            int level = getLevel(damagee);
            if (level > 0) {
                double modifier = level * 15;
                double modifier2 = modifier >= 10 ? 0.01 : 0.1;

                event.setDamage(event.getDamage() * (1.0 - (modifier * modifier2)));
            }
        }


        if (event.getDamager() instanceof Player damager) {
            int level = getLevel(damager);
            if(level > 0) {
                double modifier = level * 15;
                double modifier2 = modifier >= 10 ? 0.01 : 0.1;

                event.setDamage(event.getDamage() * (1.0 - (modifier * modifier2)));
            }

        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

}
