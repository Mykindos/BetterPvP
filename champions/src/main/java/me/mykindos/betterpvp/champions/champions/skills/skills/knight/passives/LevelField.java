package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;


import java.util.*;

@Singleton
@BPvPListener
public class LevelField extends Skill implements ToggleSkill, CooldownSkill, Listener {

    private double radius;
    private double damageTakenPerPlayer;
    private double damageDealtPerPlayer;
    private int duration;
    private int maxEnemies;
    private HashMap<UUID, Integer> lockedValues = new HashMap<>();
    private HashMap<UUID, Long> toggleTimestamps = new HashMap<>();

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();

        int currentPlayers = Math.min(nearbyEnemies, maxEnemies);

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(currentPlayers)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxEnemies - currentPlayers))).color(NamedTextColor.GRAY));
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
                "For every enemy that outnumbers you within <stat>" + radius + "</stat> blocks, you",
                "",
                "Deal: <val>" + (level * damageDealtPerPlayer) + "</val> more damage",
                "Take: <val>" + (level * damageTakenPerPlayer) + "</val> less damage",
                "",
                "Drop weapon to lock in values for the next 5 seconds",
                "",
                "Maximum number of enemies: <stat>"+ maxEnemies,
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2.5);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if(!(level > 0)) return;

        int nearbyDifference;
        if(lockedValues.containsKey(player.getUniqueId())) {
            nearbyDifference = lockedValues.get(player.getUniqueId());
        } else {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size() + 1;
            nearbyDifference = (nearbyEnemies - nearbyAllies);
        }

        double damageAdded = Math.min(damageDealtPerPlayer * maxEnemies, Math.max(0, (nearbyDifference * (damageDealtPerPlayer * level))));
        event.setDamage(event.getDamage() + damageAdded);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event){
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;

        int level = getLevel(player);
        if(!(level > 0)) return;

        int nearbyDifference;
        if(lockedValues.containsKey(player.getUniqueId())) {
            nearbyDifference = lockedValues.get(player.getUniqueId());
        } else {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size() + 1;
            nearbyDifference = (nearbyEnemies - nearbyAllies);
        }

        double damageSubtracted = Math.max((damageTakenPerPlayer * maxEnemies) * -1, Math.min(0, (nearbyDifference * -1) * (damageTakenPerPlayer * level)));
        event.setDamage(event.getDamage() + damageSubtracted);
    }

    @Override
    public void toggle(Player player, int level) {
        int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
        int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size() + 1;
        int nearbyDifference = (nearbyEnemies - nearbyAllies);

        lockedValues.put(player.getUniqueId(), nearbyDifference);
        toggleTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        long currentTime = System.currentTimeMillis();
        toggleTimestamps.forEach((uuid, timestamp) -> {
            if (currentTime - timestamp >= duration * 1000) {
                lockedValues.remove(uuid);
                toggleTimestamps.remove(uuid);
            }
        });
    }

    public void loadSkillConfig() {
        radius = getConfig("radius", 5.0, Double.class);
        damageDealtPerPlayer = getConfig("damagePerPlayer", 0.5, Double.class);
        damageTakenPerPlayer = getConfig("damagePerPlayer", 0.5, Double.class);
        duration = getConfig("duration", 10, Integer.class);
        maxEnemies = getConfig("maxEnemies", 8, Integer.class);
    }
}
