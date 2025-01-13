package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class SmokeBomb extends Skill implements CooldownToggleSkill, Listener, DebuffSkill, DefensiveSkill {

    private final Map<UUID, Long> smoked = new HashMap<>();

    @Getter
    private double duration;

    @Getter
    private double blindDuration;
    @Getter
    private double blindRadius;
    private boolean allowPickupItems;

    @Inject
    public SmokeBomb(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Smoke Bomb";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Instantly <effect>Vanish</effect> before your foes",
                "for a maximum of <val>" + getDuration() + "</val> seconds,",
                "inflicting <effect>Blindness</effect> to enemies",
                "within <val>" + getBlindRadius() + "</val> blocks for <val>" + getBlindDuration() + "</val> seconds",
                "",
                "Interacting with your surroundings",
                "or taking damage",
                "will cause you to reappear",
                "",
                "Cooldown: <val>" + getCooldown(),
                "",
                EffectTypes.VANISH.getDescription(0)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void toggle(Player player) {
        // Effects
        championsManager.getEffects().addEffect(player, EffectTypes.VANISH, getName(), 1, (long) (getDuration() * 1000L));
        smoked.put(player.getUniqueId(), System.currentTimeMillis());
        for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), blindRadius)) {
            championsManager.getEffects().addEffect(target, player, EffectTypes.BLINDNESS, 1, (long) (blindDuration * 1000L));
        }
        championsManager.getEffects().addEffect(player, player, EffectTypes.BLINDNESS, 1, (long) (blindDuration * 1000L));

        // Display particle to those only within 30 blocks
        Particle.EXPLOSION_EMITTER.builder()
                .location(player.getLocation())
                .receivers(30)
                .spawn();
        Particle.SQUID_INK.builder()
                .location(player.getLocation())
                .receivers(30)
                .extra(0)
                .count(10)
                .offset(3, 3, 3)
                .spawn();

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);
        for (int i = 0; i < 3; i++) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 2.0F, 0.5F);
        }
    }

    private void reappear(Player player) {
        championsManager.getEffects().removeEffect(player, EffectTypes.VANISH, getName());

        Particle.GLOW_SQUID_INK.builder()
                .location(player.getLocation())
                .receivers(30)
                .extra(0)
                .count(10)
                .offset(3, 3, 3)
                .spawn();

        player.playSound(player.getLocation().add(0, 1, 0), Sound.ENTITY_ALLAY_HURT, 0.5F, 0.5F);
        UtilMessage.message(player, getClassType().getName(), "You have reappeared.");
    }

    private void interact(Player player) {
        final long castTime = smoked.get(player.getUniqueId());
        if (!UtilTime.elapsed(castTime, 100L)) {
            return;
        }

        smoked.remove(player.getUniqueId());
        reappear(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (smoked.containsKey(player.getUniqueId())) {
            interact(player);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (allowPickupItems) return;
        Player player = event.getPlayer();
        if (smoked.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (smoked.containsKey(player.getUniqueId())) {
            interact(player);
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player player && smoked.containsKey(player.getUniqueId())
                && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            smoked.remove(player.getUniqueId());
            reappear(player);
        }
        if (event.getDamagee() instanceof Player player && smoked.containsKey(player.getUniqueId())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                // While smoke bombed, cancel melee damage from enemies
                event.setCancelled(true);
            } else if (event.getCause() != EntityDamageEvent.DamageCause.POISON
                    && !event.hasReason("Bleed")
                    && event.getCause() != EntityDamageEvent.DamageCause.FIRE
                    && event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK) {
                smoked.remove(player.getUniqueId());
                reappear(player);
            }

        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = smoked.keySet().iterator();
        while (it.hasNext()) {
            final Player player = Bukkit.getPlayer(it.next());
            if (player == null || !player.isValid() || player.isDead()) {
                it.remove();
                continue;
            }

            // Remove if expire
            final long castTime = smoked.get(player.getUniqueId());
            if (!hasSkill(player) || UtilTime.elapsed(castTime, (long) (getDuration() * 1000L))) {
                reappear(player);
                it.remove();
                continue;
            }

            // Passive particles
            final double random = Math.random();
            if (random < 0.1) {
                player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.STEP_SOUND, 0, 60);
            }
            if (random < 0.3) {
                Particle.SMOKE.builder()
                        .location(player.getLocation())
                        .receivers(30)
                        .offset(0, 0.2, 0)
                        .count(5)
                        .extra(0)
                        .spawn();
            }
        }

    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 4.0, Double.class);
        blindDuration = getConfig("blindDuration", 1.75, Double.class);
        blindRadius = getConfig("blindRadius", 4.0, Double.class);
        allowPickupItems = getConfig("allowPickupItems", false, Boolean.class);
    }

}
