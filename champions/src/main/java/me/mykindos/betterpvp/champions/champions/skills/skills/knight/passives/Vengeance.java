package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
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
public class Vengeance extends Skill implements PassiveSkill, Listener, OffensiveSkill, DamageSkill {
    private final WeakHashMap<Player, Integer> playerNumHitsMap = new WeakHashMap<>();
    private final WeakHashMap<Player, BukkitTask> playerTasks = new WeakHashMap<>();
    @Getter
    private double damage;
    @Getter
    private double maxDamage;
    @Getter
    private double expirationTime;

    @Inject
    public Vengeance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vengeance";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "For every hit you took since last damaging",
                "an enemy, your damage will increase by <val>" + getDamage() + "</val> damage",
                "up to a maxiumum of <val>" + getMaxDamage() + "</val> extra damage",
                "",
                "Extra damage will reset after " + getExpirationTime() + " seconds"
        };
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
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        if (event.getDamager() instanceof Player) {
            int numHits = playerNumHitsMap.getOrDefault(player, 0);
            numHits++;
            playerNumHitsMap.put(player, numHits);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;

        if (hasSkill(player)) {
            int numHitsTaken = playerNumHitsMap.getOrDefault(player, 0);
            double damageIncrease = Math.min(getMaxDamage(), numHitsTaken * getDamage());

            event.setDamage(event.getDamage() + damageIncrease);

            playerNumHitsMap.put(player, 0);

            if (playerTasks.containsKey(player)) {
                playerTasks.get(player).cancel();
                playerTasks.remove(player);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(champions, () -> {
                playerNumHitsMap.put(player, 0);
                playerTasks.remove(player);
            }, (long) getExpirationTime() * 20L);

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
        damage = getConfig("damage", 0.75, Double.class);
        maxDamage = getConfig("maxDamage", 1.5, Double.class);
        expirationTime = getConfig("expirationTime", 6.0, Double.class);
    }

}
