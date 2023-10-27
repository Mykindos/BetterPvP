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

import java.util.Set;

@Singleton
@BPvPListener
public class Impotence extends Skill implements PassiveSkill {

    private int radius;
    private int maxEnemies;
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
                "For each enemy within <val>" + (radius + level) + "</val> blocks you take",
                "reduced damage from all sources, at a",
                "maximum of <stat>" + maxEnemies + "</stat> players",
                "",
                "Damage Reduction:",
                "1 nearby enemy = <stat>" + (calculateReduction(1) * 100)  + "%</stat>",
                "2 nearby enemies = <stat>" + (calculateReduction(2) * 100) + "%</stat>",
                "3 nearby enemies = <stat>" + (calculateReduction(3) * 100) + "%</stat>"
        };
    }
    @Override
    public String getDefaultClassString() {
        return "warlock";
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
            int nearby = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius + level).size();
            event.setDamage(event.getDamage() * (1 - calculateReduction(nearby)));
        }
    }

    private double calculateReduction(int nearby) {
        double rawDecrease = 15 + (Math.min(nearby, maxEnemies) * 5);
        return rawDecrease * 0.01;
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 3, Integer.class);
        maxEnemies = getConfig("maxEnemies", 3, Integer.class);
    }
}
