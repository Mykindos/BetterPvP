package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class SmokeBomb extends Skill implements CooldownToggleSkill, Listener {

    private final Map<UUID, Long> smoked = new HashMap<>();
    private double duration;
    private int effectDuration;
    private double effectRadius;
    private int effectDurationIncreasePerLevel;
    private double effectRadiusIncreasePerLevel;
    private int slownessLevel;

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
                "Instantly Vanish before your foes",
                "for <stat>" + duration + "</stat> seconds, inflicting <effect>Blindness",
                "and <effect>Slowness " + UtilFormat.getRomanNumeral(slownessLevel + 1) + "</effect> to enemies within <stat>" + getRadius(level),
                "blocks for <val>" + getEffectDuration(level) + "</val seconds",
                "",
                "Interacting or taking damage will",
                "cause you to reappear",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    public void loadSkillConfig() {
        duration = getConfig("duration", 3.0, Double.class);
        effectDurationIncreasePerLevel = getConfig("effectDurationIncreasePerLevel", 1, Integer.class);
        effectDuration = getConfig("effectDuration", 5, Integer.class);
        effectRadius = getConfig("effectRadius", 5.0, Double.class);
        effectRadiusIncreasePerLevel = getConfig("effectRadiusIncreasePerLevel", 1.0, Double.class);
        slownessLevel = getConfig("slownessLevel", 1, Integer.class);
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    public double getEffectDuration(int level) {
        return effectDuration + ((level-1) * effectDurationIncreasePerLevel);
    }
    public double getRadius(int level){
        return effectRadius + ((level - 1) * effectRadiusIncreasePerLevel);
    }

    @Override
    public void toggle(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectType.INVISIBILITY, (long) (duration * 1000L));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) (duration * 20), 0, false, false));
        smoked.put(player.getUniqueId(), System.currentTimeMillis());
        for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), getRadius(level))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) ((getEffectDuration(level) + 1.0) * 20), 0)); //extra second because of the blindness fade at the end
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getEffectDuration(level) * 20), slownessLevel));
            championsManager.getEffects().addEffect(target, EffectType.CONFUSED, (long) getEffectDuration(level) * 1000L);
        }

        // Display particle to those only within 30 blocks
        Particle.EXPLOSION_HUGE.builder()
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

        for (int i = 0; i < 3; i++) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 2.0F, 0.5F);
        }
    }

    private void reappear(Player player) {
        championsManager.getEffects().removeEffect(player, EffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        UtilServer.callEvent(new EffectExpireEvent(player, new Effect(player.getUniqueId().toString(), EffectType.INVISIBILITY, 1, 0))); // Do this incase
        UtilMessage.message(player, getClassType().getName(), "You have reappeared.");
    }

    private void interact(Player player) {
        final long castTime = smoked.get(player.getUniqueId());
        if (!UtilTime.elapsed(castTime, 25L)) {
            return;
        }

        smoked.remove(player.getUniqueId());
        reappear(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (smoked.containsKey(player.getUniqueId())) {
            interact(player);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) { //bit suspicious
        Player player = event.getPlayer();
        if (smoked.containsKey(player.getUniqueId())) {
            interact(player);
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
            smoked.remove(player.getUniqueId());
            reappear(player);
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
            final int level = getLevel(player);
            if (level <= 0 || UtilTime.elapsed(castTime, (long) duration * 1000L)) {
                reappear(player);
                it.remove();
            }
        }
    }
}
