package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
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
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class SmokeBomb extends Skill implements CooldownToggleSkill, Listener, DebuffSkill, DefensiveSkill {

    private final Map<UUID, Long> smokedPlayers = new HashMap<>();
    private final WeakHashMap<Player, Long> deactivationDelays = new WeakHashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private double blindDuration;
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
    public String[] getDescription(int level) {
        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Instantly <effect>Vanish</effect> before your foes",
                "for a maximum of " + getValueString(this::getDuration, level) + " seconds,",
                "inflicting <effect>Blindness</effect> to enemies",
                "within " + getValueString(this::getBlindRadius, level) + " blocks for <stat>" + getValueString(this::getBlindDuration, level) + " seconds",
                "",
                "Interacting with your surroundings",
                "or taking damage",
                "will cause you to reappear",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.VANISH.getDescription(0)
        };
    }

    public double getBlindRadius(int level) {
        return blindRadius;
    }

    public double getBlindDuration(int level) {
        return blindDuration;
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    @Override
    public void toggle(Player player, int level) {
        applyVanishEffect(player, level);
        applyBlindnessToNearbyEnemies(player);
        playActivationEffects(player);
    }

    private void applyVanishEffect(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectTypes.VANISH, getName(), 1,
                (long) (getDuration(level) * 1000L));
        smokedPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        deactivationDelays.put(player, System.currentTimeMillis() + 100L);
    }

    private void applyBlindnessToNearbyEnemies(Player player) {
        for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), blindRadius)) {
            championsManager.getEffects().addEffect(target, player, EffectTypes.BLINDNESS, 1,
                    (long) (blindDuration * 1000L));
        }
    }

    private void playActivationEffects(Player player) {
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

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.0f);
        for (int i = 0; i < 3; i++) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 2.0F, 0.5F);
        }
    }

    private void reappear(Player player) {
        championsManager.getEffects().removeEffect(player, EffectTypes.VANISH, getName());
        messageAppear(player);
        deactivationDelays.remove(player);
    }

    private void messageAppear(Player player) {
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
        final long castTime = smokedPlayers.get(player.getUniqueId());
        if (!UtilTime.elapsed(castTime, 100L)) {
            return;
        }

        if (isInDeactivationDelay(player)) return;

        smokedPlayers.remove(player.getUniqueId());
        reappear(player);
    }

    private boolean isInDeactivationDelay(Player player) {
        return deactivationDelays.getOrDefault(player, 0L) > System.currentTimeMillis();
    }

    private boolean isSmoked(Player player) {
        return smokedPlayers.containsKey(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isSmoked(player)) {
            interact(player);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (allowPickupItems) return;
        Player player = event.getPlayer();
        if (isSmoked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (isSmoked(player)) {
            interact(player);
        }
    }

    @EventHandler
    public void onEffectExpire(EffectExpireEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (getName().equals(event.getEffect().getName())) {
            if (isSmoked(player)) {
                smokedPlayers.remove(player.getUniqueId());
                messageAppear(player);
            }
        }
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        handleDamagerVisibility(event);
        handleDamageeVisibility(event);
    }

    private void handleDamagerVisibility(DamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isSmoked(player)) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;

        if (isInDeactivationDelay(player)) {
            event.setCancelled(true);
            return;
        }

        smokedPlayers.remove(player.getUniqueId());
        reappear(player);
    }

    private void handleDamageeVisibility(DamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!isSmoked(player)) return;

        if (event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) {
            // While smoke bombed, cancel melee damage from enemies
            event.setCancelled(true);
        } else if (shouldRevealOnDamage(event)) {
            smokedPlayers.remove(player.getUniqueId());
            reappear(player);
        }
    }

    private boolean shouldRevealOnDamage(DamageEvent event) {
        return event.getBukkitCause() != EntityDamageEvent.DamageCause.POISON
                && !event.hasReason("Bleed")
                && event.getBukkitCause() != EntityDamageEvent.DamageCause.FIRE
                && event.getBukkitCause() != EntityDamageEvent.DamageCause.FIRE_TICK;
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = smokedPlayers.keySet().iterator();
        while (it.hasNext()) {
            final Player player = Bukkit.getPlayer(it.next());
            if (player == null || !player.isValid() || player.isDead()) {
                it.remove();
                continue;
            }

            if (shouldExpireEffect(player)) {
                it.remove();
                reappear(player);
                continue;
            }

            displayPassiveEffects(player);
        }
    }

    private boolean shouldExpireEffect(Player player) {
        final long castTime = smokedPlayers.get(player.getUniqueId());
        final int level = getLevel(player);
        return level <= 0 || UtilTime.elapsed(castTime, (long) (getDuration(level) * 1000L));
    }

    private void displayPassiveEffects(Player player) {
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

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 4.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        blindDuration = getConfig("blindDuration", 1.75, Double.class);
        blindRadius = getConfig("blindRadius", 4.0, Double.class);
        allowPickupItems = getConfig("allowPickupItems", false, Boolean.class);
    }
}