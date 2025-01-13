package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class LevelField extends Skill implements Listener, DefensiveSkill, OffensiveSkill, DamageSkill {

    private double radius;
    private double radiusIncreasePerLevel;
    private int maxEnemies;
    private int maxEnemiesIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseDamageReduced;
    private double damagedReducedPerLevel;

    private HashMap<UUID, Integer> playerNearbyDifferenceMap = new HashMap<>();

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        Player player = gamer.getPlayer();
        if (player == null || getLevel(player) <= 0) {
            return null;
        }

        int level = getLevel(player);
        int totalSquares = getMaxEnemies(level);
        Integer nearbyDifference = playerNearbyDifferenceMap.getOrDefault(player.getUniqueId(), 0);

        int squares;
        NamedTextColor color;
        if (nearbyDifference >= 0) {
            squares = Math.min(nearbyDifference, totalSquares);
            color = NamedTextColor.RED;
        } else {
            squares = Math.min(-nearbyDifference, totalSquares);
            color = NamedTextColor.GREEN;
        }

        return Component.text("")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(squares)).color(color))
                .append(Component.text("\u25A0".repeat(Math.max(0, totalSquares - squares))).color(NamedTextColor.GRAY));
    });

    @Inject
    public LevelField(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Level Field";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For every nearby enemy",
                "outnumbering nearby allies",
                "Your damage is increased by " + getValueString(this::getDamage, level),
                "And you take " + getValueString(this::getDamageReduction, level) + " less damage",
                "",
                "Damage can be altered by a maximum of " + getValueString(this::getMaxEnemies, level),
                "",
                "Radius: " + getValueString(this::getRadius, level),
        };
    }

    public int getMaxEnemies(int level) {
        return maxEnemies + ((level - 1) * maxEnemiesIncreasePerLevel);
    }

    public double getRadius(int level) {
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }
    private double getDamageReduction(int level) {
        return baseDamageReduced + ((level - 1) * damagedReducedPerLevel);
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @Override
    public SkillType getType() {
        return SkillType.GLOBAL;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageReceive(CustomDamageEvent event) {
        if(!(event.getDamagee() instanceof Player defender)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        int level = getLevel(defender);
        if (level > 0) {
            processLevelFieldSkill(defender, event, false, level);
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onDamageDeal(CustomDamageEvent event) {
        if(!(event.getDamager() instanceof Player attacker)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        var level = getLevel(attacker);
        if (level > 0) {
            processLevelFieldSkill(attacker, event, true, level);
        }
    }

    private void processLevelFieldSkill(Player relevantPlayer, CustomDamageEvent event, boolean isAttacker, int level) {
        List<LivingEntity> nearbyEnemiesList = UtilEntity.getNearbyEnemies(relevantPlayer, relevantPlayer.getLocation(), radius);
        nearbyEnemiesList.removeIf(e -> !(e instanceof Monster) && !(e instanceof Player));
        int nearbyEnemies = nearbyEnemiesList.size();
        int nearbyAllies = UtilPlayer.getNearbyAllies(relevantPlayer, relevantPlayer.getLocation(), radius).size() + 1;
        int nearbyDifference = nearbyEnemies - nearbyAllies;

        if (nearbyDifference < 1) return;

        double damageMod = Math.min(nearbyDifference, getMaxEnemies(level));

        if (isAttacker) {
            event.setDamage(event.getDamage() + damageMod * getDamage(level));
        } else {
            event.setDamage(event.getDamage() - damageMod * getDamageReduction(level));
        }
    }

    @UpdateEvent(delay = 250)
    public void updateDisplay() {
        HashMap<UUID, Integer> updatedMap = new HashMap<>();

        for (UUID playerUUID : playerNearbyDifferenceMap.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            int level = getLevel(player);
            if (level <= 0) continue;

            double radius = getRadius(level);
            if (player.isOnline()) {
                List<KeyValue<LivingEntity, EntityProperty>> nearbyEntities = UtilEntity.getNearbyEntities(player, radius);
                int nearbyEnemies = (int) nearbyEntities.stream().filter(k -> k.getValue() == EntityProperty.ENEMY).count();
                int nearbyAllies = (int) nearbyEntities.stream().filter(k -> k.getValue() == EntityProperty.FRIENDLY).count() + 1;
                int nearbyDifference = nearbyEnemies - nearbyAllies;

                updatedMap.put(playerUUID, nearbyDifference);

            }
        }
        playerNearbyDifferenceMap = updatedMap;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
        playerNearbyDifferenceMap.remove(player.getUniqueId());
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(1200, actionBarComponent);
        playerNearbyDifferenceMap.put(player.getUniqueId(), 0);

    }

    public void loadSkillConfig() {
        radius = getConfig("radius", 6.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 2.0, Double.class);
        maxEnemies = getConfig("maxEnemies", 2, Integer.class);
        maxEnemiesIncreasePerLevel = getConfig("maxEnemiesIncreasePerLevel", 1, Integer.class);
        baseDamage = getConfig("baseDamage", 1.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.0, Double.class);
        baseDamageReduced = getConfig("baseDamageReduced", 1.0, Double.class);
        damagedReducedPerLevel = getConfig("damagedReducedPerLevel", 0.0, Double.class);
    }
}
