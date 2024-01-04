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
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
@BPvPListener
public class Curse extends Skill implements PassiveSkill, Listener {

    private int curseDuration;
    private double internalCD;
    private int effectLevel;
    private final HashMap<UUID, Set<UUID>> cursedPlayers;
    private final HashMap<UUID, Long> lastCurseTime;
  
    @Inject
    public Curse(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
        this.cursedPlayers = new HashMap<>();
        this.lastCurseTime = new HashMap<>();
    }


    @Override
    public String getName() {
        return "Curse";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Hitting players will Curse them for <val>" + getCurseDuration(level),
                "seconds, afflicting them with <effect>Vulnerability " + UtilFormat.getRomanNumeral(effectLevel + 1),
                "",
                "If you take damage, Curse will be removed from all targets",
                "",
                "Internal Cooldown: " + internalCD
        };
    }

    public int getCurseDuration(int level){
        return (curseDuration + (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(CustomDamageEvent event) {
        if (event.isCancelled() || event.getCause() != DamageCause.ENTITY_ATTACK) return;

        if (!(event.getDamager() instanceof Player attacker) || !(event.getDamagee() instanceof Player target)) return;

        int level = getLevel(attacker);
        if (level <= 0) return;

        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();

        long currentTime = System.currentTimeMillis();
        lastCurseTime.putIfAbsent(attackerId, 0L);
        if (currentTime - lastCurseTime.get(attackerId) < internalCD * 1000) {
            return;
        }

        cursedPlayers.computeIfAbsent(attackerId, k -> new HashSet<>()).add(targetId);
        championsManager.getEffects().addEffect(target, EffectType.VULNERABILITY, effectLevel, (long) ((getCurseDuration(level) + level) * 1000L));
        lastCurseTime.put(attackerId, currentTime);

        Bukkit.getScheduler().runTaskLater(champions, () -> {
            if (cursedPlayers.getOrDefault(attackerId, new HashSet<>()).contains(targetId)) {
                championsManager.getEffects().removeEffect(target, EffectType.VULNERABILITY);
                cursedPlayers.get(attackerId).remove(targetId);
            }
        }, getCurseDuration(level) * 20L);
    }

    @EventHandler
    public void onDamageTaken(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();
        if (cursedPlayers.containsKey(playerId)) {
            Set<UUID> targets = cursedPlayers.get(playerId);
            for (UUID targetId : targets) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    championsManager.getEffects().removeEffect(target, EffectType.VULNERABILITY);
                }
            }
            cursedPlayers.remove(playerId);
        }
    }
    public void loadSkillConfig() {
        curseDuration = getConfig("curseDuration", 3, Integer.class);
        internalCD = getConfig("internalCD", 3.0, Double.class);
        effectLevel = getConfig("effectLevel", 1, Integer.class);
    }
}
