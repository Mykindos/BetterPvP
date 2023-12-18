package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

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
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Vengeance extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<Player, Integer> playerNumHitsMap = new WeakHashMap<>();
    private final WeakHashMap<Player, BukkitTask> playerTasks = new WeakHashMap<>();

    private double baseDamage;
    private double damageIncreasePerLevel;

    private double baseMaxDamage;

    private double maxDamageIncreasePerLevel;

    @Inject
    public Vengeance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vengeance";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "For every subsequent hit, your damage",
                "will increase by <val>" + getDamage(level) + "</val>",
                "",
                "If you take damage, your damage will reset",
                "",
                "you can deal a maximum of <val>" + getMaxDamage(level) + "</val> extra damage"
        };
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + level * damageIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
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
            int numHits = playerNumHitsMap.getOrDefault(player, 0);
            if (numHits > 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, (float) 2.0, (float) 1.5);
            }
            playerNumHitsMap.put(player, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        int numHits = playerNumHitsMap.getOrDefault(player, 0);
        numHits++;
        playerNumHitsMap.put(player, numHits);

        int level = getLevel(player);
        if (level > 0) {
            double damageIncrease = Math.min(getMaxDamage(level), (numHits - 1) * getDamage(level));
            if (damageIncrease > 0) {
                event.setDamage(event.getDamage() + damageIncrease);
                UtilMessage.simpleMessage(player, getClassType().getName(), "%s: <yellow>+%2.2f<gray> Bonus Damage", getName(), damageIncrease);
            }

            if (playerTasks.containsKey(player)) {
                playerTasks.get(player).cancel();
                playerTasks.remove(player);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(champions, () -> {
                playerNumHitsMap.put(player, 0);
                playerTasks.remove(player);
                if (!player.isDead()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, (float) 2.0, (float) 1.5);
                }
            }, 100L);

            playerTasks.put(player, task);
        }

    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        playerNumHitsMap.put(player, 0);
        if (playerTasks.containsKey(player)) {
            playerTasks.get(player).cancel();
            playerTasks.remove(player);
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 0.25, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.25, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 1.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }

}
