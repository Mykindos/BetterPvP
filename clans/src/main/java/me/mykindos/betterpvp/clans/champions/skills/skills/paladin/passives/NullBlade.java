package me.mykindos.betterpvp.clans.champions.skills.skills.paladin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class NullBlade extends Skill implements PassiveSkill, EnergySkill {

    @Inject
    public NullBlade(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Null Blade";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your sword sucks " + ChatColor.GREEN + getEnergy(level) + ChatColor.GRAY + " energy from",
                "opponents with every attack"
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player dam)) return;

        int level = getLevel(dam);
        if (level > 0) {
            double degeneration = getEnergy(level) * 0.01;

            championsManager.getEnergy().degenerateEnergy(target, degeneration);
            championsManager.getEnergy().regenerateEnergy(dam, degeneration);
        }


    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @Override
    public float getEnergy(int level) {

        return energy + ((level - 1) * 2);
    }


}
