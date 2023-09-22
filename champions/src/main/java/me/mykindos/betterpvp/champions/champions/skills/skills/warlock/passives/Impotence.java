package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

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
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Impotence extends Skill implements PassiveSkill {

    @Inject
    public Impotence(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Impotence";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For each enemy within <val>" + (3 + level) + "</val> blocks",
                "you take reduced damage from all sources, ",
                "at a maximum of <val>3</val> players.",
                "",
                "Damage Reduction:",
                "1 nearby enemy = <val>20%</val>",
                "3 nearby enemies = <val>30%</val>"
        };
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        int level = getLevel(player);
        if (level > 0) {
            int nearby = UtilPlayer.getNearbyEnemies(player, player.getLocation(), 3 + level).size();
            event.setDamage(event.getDamage() * (1 - ((15 + (Math.min(nearby, 3) * 5)) * 0.01)));
        }

    }
}
