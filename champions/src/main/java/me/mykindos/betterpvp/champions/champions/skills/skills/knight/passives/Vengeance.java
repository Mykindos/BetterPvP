package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseMaxDamage;
    private double maxDamageIncreasePerLevel;
    private double expirationTime;
    private double expirationTimeIncreasePerLevel;

    @Inject
    public Vengeance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Vengeance";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component maxDamage = getValueComponent(this::getMaxDamage, level);
        Component expiration = getValueComponent(this::getExpirationTime, level);
        return Translations.componentLines(
                "champions.skill.knight.vengeance.description",
                damage,
                maxDamage,
                expiration
        );
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level - 1) * maxDamageIncreasePerLevel);
    }

    public double getExpirationTime(int level) {
        return expirationTime;
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
    public void onDamage(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        if (event.getDamager() instanceof Player) {
            int numHits = playerNumHitsMap.getOrDefault(player, 0);
            numHits++;
            playerNumHitsMap.put(player, numHits);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(DamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.isCancelled()) return;
        if (event.getBukkitCause() != DamageCause.ENTITY_ATTACK) return;

        int level = getLevel(player);
        if (level > 0) {
            int numHitsTaken = playerNumHitsMap.getOrDefault(player, 0);
            double damageIncrease = Math.min(getMaxDamage(level), numHitsTaken * getDamage(level));

            event.addModifier(new SkillDamageModifier.Flat(this, damageIncrease));

            playerNumHitsMap.put(player, 0);

            if (playerTasks.containsKey(player)) {
                playerTasks.get(player).cancel();
                playerTasks.remove(player);
            }

            BukkitTask task = Bukkit.getScheduler().runTaskLater(champions, () -> {
                playerNumHitsMap.put(player, 0);
                playerTasks.remove(player);
            }, (long) getExpirationTime(level) * 20L);

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
        baseDamage = getConfig("baseDamage", 0.75, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.25, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 1.5, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 0.5, Double.class);
        expirationTime = getConfig("expirationTime", 6.0, Double.class);
        expirationTimeIncreasePerLevel = getConfig("expirationTimeIncreasePerLevel", 0.0, Double.class);
    }

}
