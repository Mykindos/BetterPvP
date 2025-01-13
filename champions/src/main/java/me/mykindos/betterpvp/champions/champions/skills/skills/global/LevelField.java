package me.mykindos.betterpvp.champions.champions.skills.skills.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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
import org.bukkit.entity.Chicken;
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

    @Getter
    private double radius;
    @Getter
    private int maxEnemies;
    @Getter
    private double damage;
    private double damageReduced;

    private HashMap<UUID, Integer> playerNearbyDifferenceMap = new HashMap<>();

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        Player player = gamer.getPlayer();
        if (player == null || !hasSkill(player)) {
            return null;
        }

        int totalSquares = getMaxEnemies();
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
    public String[] getDescription() {
        return new String[]{
                "For every nearby enemy",
                "outnumbering nearby allies",
                "Your damage is increased by <val>" + getDamage(),
                "And you take <val>" + getDamageReduction() + "</val> less damage",
                "",
                "Damage can be altered by a maximum of <val>" + getMaxEnemies(),
                "",
                "Radius: <val>" + getRadius(),
        };
    }

    private double getDamageReduction() {
        return damageReduced;
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
        if (!(event.getDamagee() instanceof Player defender)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        if (hasSkill(defender)) {
            processLevelFieldSkill(defender, event, false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageDeal(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        if (hasSkill(attacker)) {
            processLevelFieldSkill(attacker, event, true);
        }
    }

    private void processLevelFieldSkill(Player relevantPlayer, CustomDamageEvent event, boolean isAttacker) {
        List<LivingEntity> nearbyEnemiesList = UtilEntity.getNearbyEnemies(relevantPlayer, relevantPlayer.getLocation(), radius);
        nearbyEnemiesList.removeIf(e -> e instanceof Chicken || e.hasMetadata("AlmPet") || e.hasMetadata("PlayerSpawned"));
        int nearbyEnemies = nearbyEnemiesList.size();
        int nearbyAllies = UtilPlayer.getNearbyAllies(relevantPlayer, relevantPlayer.getLocation(), radius).size() + 1;
        int nearbyDifference = nearbyEnemies - nearbyAllies;

        if (nearbyDifference < 1) return;

        double damageMod = Math.min(nearbyDifference, getMaxEnemies());

        if (isAttacker) {
            event.setDamage(event.getDamage() + damageMod * getDamage());
        } else {
            event.setDamage(event.getDamage() - damageMod * getDamageReduction());
        }
    }

    @UpdateEvent(delay = 250)
    public void updateDisplay() {
        HashMap<UUID, Integer> updatedMap = new HashMap<>();

        for (UUID playerUUID : playerNearbyDifferenceMap.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null) continue;

            if (!hasSkill(player)) continue;

            double radius = getRadius();
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
        gamer.getActionBar().add(1100, actionBarComponent);
        playerNearbyDifferenceMap.put(player.getUniqueId(), 0);

    }

    public void loadSkillConfig() {
        radius = getConfig("radius", 6.0, Double.class);
        maxEnemies = getConfig("maxEnemies", 2, Integer.class);
        damage = getConfig("damage", 1.0, Double.class);
        damageReduced = getConfig("damageReduced", 1.0, Double.class);
    }
}
