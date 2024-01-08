package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@BPvPListener
public class LevelField extends Skill implements ToggleSkill, CooldownSkill, Listener {

    private double radius;
    private double damageTakenPerPlayer;
    private double damageDealtPerPlayer;
    private int duration;
    private int maxEnemies;
    private double levelFieldUpdateTime;
    private HashMap<UUID, Integer> lockedValues = new HashMap<>();
    private HashMap<UUID, Long> toggleTimestamps = new HashMap<>();
    private ConcurrentHashMap<UUID, Integer> nearbyEnemiesCount = new ConcurrentHashMap<>();
    private Set<UUID> playersWithSkill = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private int updateTaskId = -1;

    public int getLevelFieldUpdateTime(){
        return (int)(levelFieldUpdateTime * 20);
    }

    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        Player player = gamer.getPlayer();
        if (player == null || getLevel(player) <= 0) {
            return null;
        }

        Integer lockedCount = lockedValues.get(player.getUniqueId());
        Integer count;

        if (lockedCount != null) {
            count = lockedCount;
        } else {
            count = nearbyEnemiesCount.getOrDefault(player.getUniqueId(), 0);
        }

        int displaySquares = Math.min(count, maxEnemies);

        return Component.text("")
                .color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(displaySquares)).color(NamedTextColor.RED))
                .append(Component.text("\u25A0".repeat(Math.max(0, maxEnemies - displaySquares))).color(NamedTextColor.GRAY));
    });

    private void startPeriodicEnemyCountUpdate() {
        updateTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(champions, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playersWithSkill.contains(player.getUniqueId())) {
                    Bukkit.getScheduler().runTask(champions, () -> {
                        try {
                            int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
                            int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size();
                            int nearbyDifference = nearbyEnemies - nearbyAllies;
                            nearbyEnemiesCount.put(player.getUniqueId(), Math.max(0, (nearbyDifference - 1)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }, 0L, getLevelFieldUpdateTime()).getTaskId();
    }

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
                "Drop weapon to lock in values for the next <stat>" + duration + "</stat> seconds",
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

        return cooldown - ((level - 1));
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
        int level = getLevel(relevantPlayer);

        Integer lockedDifference = lockedValues.get(relevantPlayer.getUniqueId());
        int nearbyDifference;

        if (lockedDifference != null) {
            nearbyDifference = lockedDifference;
        } else {
            int nearbyEnemies = UtilPlayer.getNearbyEnemies(relevantPlayer, relevantPlayer.getLocation(), radius).size();
            int nearbyAllies = UtilPlayer.getNearbyAllies(relevantPlayer, relevantPlayer.getLocation(), radius).size();
            nearbyDifference = nearbyEnemies - nearbyAllies;
        }

        if (nearbyDifference <= 1) return; // no effect if not outnumbered

        double damageModifier = (nearbyDifference - 1) * (damageTakenPerPlayer * level);

        if (isAttacker) {
            event.setDamage(event.getDamage() + damageModifier);
        } else {
            event.setDamage(event.getDamage() - damageModifier);
        }
    }

    @Override
    public void toggle(Player player, int level) {
        int nearbyEnemies = UtilPlayer.getNearbyEnemies(player, player.getLocation(), radius).size();
        int nearbyAllies = UtilPlayer.getNearbyAllies(player, player.getLocation(), radius).size();
        int nearbyDifference = (nearbyEnemies - nearbyAllies);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 2.0f, 2.0f);


        lockedValues.put(player.getUniqueId(), Math.max(nearbyDifference - 1, 0));
        toggleTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @UpdateEvent(delay = 500)
    public void onUpdate() {
        long currentTime = System.currentTimeMillis();

        List<UUID> keysToRemove = new ArrayList<>();

        toggleTimestamps.forEach((uuid, timestamp) -> {
            if (currentTime - timestamp >= duration * 1000) {
                keysToRemove.add(uuid);

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                    UtilMessage.message(player, getClassType().getName(), "Level Field values reset.");
                }
            }
        });

        for (UUID key : keysToRemove) {
            lockedValues.remove(key);
            toggleTimestamps.remove(key);
        }
    }


    @Override
    public void invalidatePlayer(Player player) {
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBarComponent);
        playersWithSkill.remove(player.getUniqueId());

        if (playersWithSkill.isEmpty() && updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    @Override
    public void trackPlayer(Player player) {
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(900, actionBarComponent);
        playersWithSkill.add(player.getUniqueId());

        if (updateTaskId == -1) {
            startPeriodicEnemyCountUpdate();
        }
    }

    public void loadSkillConfig() {
        radius = getConfig("radius", 8.0, Double.class);
        damageDealtPerPlayer = getConfig("damagePerPlayer", 0.5, Double.class);
        damageTakenPerPlayer = getConfig("damagePerPlayer", 0.5, Double.class);
        duration = getConfig("duration", 7, Integer.class);
        maxEnemies = getConfig("maxEnemies", 5, Integer.class);
        levelFieldUpdateTime = getConfig("levelFieldUpdateTime", 1.0, Double.class);
    }
}
