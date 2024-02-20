package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@BPvPListener
public class LevelField extends Skill implements Listener{

    private double radius;
    private int maxEnemies;
    private int maxEnemiesIncreasePerLevel;
    private double radiusIncreasePerLevel;

    private final HashMap<UUID, Integer> playerNearbyDifferenceMap = new HashMap<>();

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        Player player = gamer.getPlayer();
        if (player == null || getLevel(player) <= 0) {
            return null;
        }

        int level = getLevel(player);
        int totalSquares = getMaxEnemies(level);
        Integer nearbyDifference = playerNearbyDifferenceMap.getOrDefault(player.getUniqueId(), 0);
        int redSquares = Math.min(nearbyDifference, totalSquares);


        return Component.text("")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(redSquares)).color(NamedTextColor.RED))
                .append(Component.text("\u25A0".repeat(Math.max(0, totalSquares - redSquares))).color(NamedTextColor.GRAY));
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
                "You deal X more damage",
                "You take X less damage",
                "X = (NearbyEnemies) - (NearbyAllies)",
                "",
                "Damage can be altered a maximum of <val>" + getMaxEnemies(level),
                "",
                "Radius: <val>" + getRadius(level)
        };
    }

    public int getMaxEnemies(int level){
        return maxEnemies + ((level - 1) * maxEnemiesIncreasePerLevel);
    }

    public double getRadius(int level){
        return radius + ((level - 1) * radiusIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event){
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        Player attacker = (event.getDamager() instanceof Player) ? (Player) event.getDamager() : null;
        Player defender = (event.getDamagee() instanceof Player) ? (Player) event.getDamagee() : null;

        if (attacker != null && getLevel(attacker) > 0) {
            processLevelFieldSkill(attacker, event, true);
        }

        if (defender != null && getLevel(defender) > 0) {
            processLevelFieldSkill(defender, event, false);
        }
    }

    private void processLevelFieldSkill(Player relevantPlayer, CustomDamageEvent event, boolean isAttacker) {
        int nearbyEnemies = UtilPlayer.getNearbyEnemies(relevantPlayer, relevantPlayer.getLocation(), radius).size();
        int nearbyAllies = UtilPlayer.getNearbyAllies(relevantPlayer, relevantPlayer.getLocation(), radius).size();
        int nearbyDifference = nearbyEnemies - nearbyAllies;

        if (nearbyDifference < 1) return;

        if (isAttacker) {
            event.setDamage(event.getDamage() + nearbyDifference);
        } else {
            event.setDamage(event.getDamage() - nearbyDifference);
        }
    }

    @UpdateEvent
    public void updateDisplay() {
        HashMap<UUID, Integer> updatedMap = new HashMap<>();

        for (UUID playerUUID : playerNearbyDifferenceMap.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            int level = getLevel(player);
            double radius = getRadius(level);
            if (player != null && player.isOnline()) {
                int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
                int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size();
                int nearbyDifference = Math.max(0,nearbyEnemies - nearbyAllies);

                updatedMap.put(playerUUID, nearbyDifference);

            }
        }
        playerNearbyDifferenceMap.clear();
        playerNearbyDifferenceMap.putAll(updatedMap);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(actionBarComponent);
        playerNearbyDifferenceMap.remove(player.getUniqueId());
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, actionBarComponent);
        playerNearbyDifferenceMap.put(player.getUniqueId(), 0);

    }

    public void loadSkillConfig() {
        radius = getConfig("radius", 4.0, Double.class);
        radiusIncreasePerLevel = getConfig("radiusIncreasePerLevel", 2.0, Double.class);
        maxEnemies = getConfig("maxEnemies", 2, Integer.class);
        maxEnemiesIncreasePerLevel = getConfig("maxEnemiesIncreasePerLevel", 1, Integer.class);
    }
}
