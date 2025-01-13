package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Impotence extends Skill implements PassiveSkill, DefensiveSkill {

    private double radius;
    private double decrease;
    private double decreasePerPlayer;
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
    public String[] getDescription() {
        return new String[]{
                "For each enemy within <val>" + getRadius() + "</val> blocks you take",
                "reduced damage from all sources, at a",
                "maximum of <val>" + getMaxEnemies() + "</val> players",
                "",
                "Damage Reduction:",
                "1 nearby enemy = <stat>" + UtilFormat.formatNumber((calculateReduction(1) * 100), 1) + "%</stat>",
                "2 nearby enemies = <stat>" + UtilFormat.formatNumber((calculateReduction(2) * 100), 1) + "%</stat>",
                "3 nearby enemies = <stat>" + UtilFormat.formatNumber((calculateReduction(3) * 100), 1) + "%</stat>"
        };
    }

    private double getRadius() {
        return radius;
    }

    private double getMaxEnemies() {
        return maxEnemies;
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
        if (!hasSkill(player)) return;

        int nearbyEnemies = UtilEntity.getNearbyEnemies(player, player.getLocation(), getRadius()).size();

        double damageReduction = 1 - calculateReduction(nearbyEnemies);
        event.setDamage(event.getDamage() * damageReduction);

        Location locationToPlayEffect = player.getLocation().add(0, 1, 0);
        player.getWorld().playEffect(locationToPlayEffect, Effect.OXIDISED_COPPER_SCRAPE, 0);
    }

    private double calculateReduction(int nearby) {
        return decrease + (Math.min(nearby, maxEnemies) * decreasePerPlayer);
    }

    @Override
    public void loadSkillConfig() {
        radius = getConfig("radius", 4.0, Double.class);
        decrease = getConfig("decrease", 0.15, Double.class);
        decreasePerPlayer = getConfig("decreasePerPlayer", 0.05, Double.class);
        maxEnemies = getConfig("maxEnemies", 3, Integer.class);
    }
}
