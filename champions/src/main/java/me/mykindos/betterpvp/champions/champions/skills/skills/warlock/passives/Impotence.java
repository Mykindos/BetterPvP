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

    private double baseRadius;
    private double radiusIncreasePerLevel;

    private double baseDecrease;

    private double baseDecreasePerPlayer;

    private double decreaseIncreasePerLevel;
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
                "For each enemy within <val>" + getRadius(level) + "</val> blocks you take",
                "reduced damage from all sources, at a",
                "maximum of <stat>" + maxEnemies + "</stat> players",
                "",
                "Damage Reduction:",
                "1 nearby enemy = <stat>" + String.format("%.1f",(calculateReduction(level, 1) * 100))  + "%</stat>",
                "2 nearby enemies = <stat>" + String.format("%.1f",(calculateReduction(level, 2) * 100)) + "%</stat>",
                "3 nearby enemies = <stat>" + String.format("%.1f",(calculateReduction(level, 3) * 100)) + "%</stat>"
        };
    }

    private double getRadius(int level) {
        return baseRadius + level * radiusIncreasePerLevel;
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
            int nearby = UtilPlayer.getNearbyEnemies(player, player.getLocation(), getRadius(level)).size();
            event.setDamage(event.getDamage() * (1 - calculateReduction(level, nearby)));
        }
    }

    private double calculateReduction(int level, int nearby) {
        return (baseDecrease + level * decreaseIncreasePerLevel) + (Math.min(nearby, maxEnemies) * baseDecreasePerPlayer);
    }

    @Override
    public void loadSkillConfig() {
        baseRadius = getConfig("baseRadius", 3.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 1.0, Double.class);

        baseDecrease = getConfig("baseDecrease", 0.15, Double.class);
        baseDecreasePerPlayer = getConfig("baseDecreasePerPlayer", 0.05, Double.class);
        decreaseIncreasePerLevel = getConfig("decreaseIncreasePerLevel", 0.0, Double.class);

        maxEnemies = getConfig("maxEnemies", 3, Integer.class);
    }
}
