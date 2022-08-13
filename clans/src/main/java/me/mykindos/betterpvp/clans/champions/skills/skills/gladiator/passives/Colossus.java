package me.mykindos.betterpvp.clans.champions.skills.skills.gladiator.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Colossus extends Skill implements PassiveSkill {

    @Inject
    public Colossus(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Colossus";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You take " + ChatColor.GREEN + (25 * level) + "% " + ChatColor.GRAY + "reduced knockback."};
    }

    @Override
    public Role getClassType() {
        return Role.GLADIATOR;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKB(CustomKnockbackEvent event) {
        if(!(event.getDamagee() instanceof Player player)) return;
        DamageCause cause = event.getCustomDamageEvent().getCause();
        if(cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.PROJECTILE) {
            int level = getLevel(player);
            if(level > 0) {
                event.setDamage(event.getDamage() * (1 - (0.25 * level)));
            }
        }

    }

}
