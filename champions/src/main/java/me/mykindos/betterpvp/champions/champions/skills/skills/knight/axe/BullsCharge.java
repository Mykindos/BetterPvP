package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BullsCharge extends Skill implements Listener, InteractSkill, CooldownSkill {

    private final HashMap<UUID, Long> running = new HashMap<>();
    private final HashMap<UUID, Long> noKnockbackTargets = new HashMap<>();


    private double speedDuration;

    private double slowDuration;

    private int speedStrength;

    private int slownessStrength;

    @Inject
    public BullsCharge(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Bulls Charge";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Enter a rage, gaining <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength + 1) + "</effect> for <stat>" + speedDuration + "</stat> seconds",
                "and giving <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength + 1) + "</effect> and no knockback to anything",
                "you hit for <stat>" + slowDuration + "</stat> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void activate(Player player, int level) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) speedDuration * 20, speedStrength));
        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 49);
        running.put(player.getUniqueId(), System.currentTimeMillis() + 4000L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player damager && running.containsKey(damager.getUniqueId())) {
            final LivingEntity damagee = event.getDamagee();

            damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (slowDuration * 20), slownessStrength));

            noKnockbackTargets.computeIfAbsent(damagee.getUniqueId(), k -> System.currentTimeMillis() + (long) (slowDuration * 1000));

            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.0F);
            damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5F, 0.5F);

            int level = getLevel(damager);
            if (damagee instanceof Player damagedPlayer) {
                UtilMessage.simpleMessage(damagedPlayer, getClassType().getName(), "<yellow>" + damager.getName() + "</yellow> hit you with <green>" + getName() + " " + level + "</green>.");
            }
            UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>" + damagee.getName() + "</yellow> with <green>" + getName() + " " + level + "</green>.");

            running.remove(damager.getUniqueId());
        }

        if (event.getDamagee() instanceof Player damagee && noKnockbackTargets.containsKey(damagee.getUniqueId())) {
            long noKnockbackEnd = noKnockbackTargets.get(damagee.getUniqueId());

            if (System.currentTimeMillis() <= noKnockbackEnd) {
                event.setKnockback(false);
            }
        }
    }




    @UpdateEvent(delay = 500)
    public void onUpdate() {
        running.entrySet().removeIf(entry -> expire(entry.getKey()));

        noKnockbackTargets.entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue());
    }

    private boolean expire(UUID uiid) {
        if (running.get(uiid) - System.currentTimeMillis() <= 0) {
            Player player = Bukkit.getPlayer(uiid);
            if (player != null) {
                int level = getLevel(player);
                UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), level));
            }
            return true;
        }
        return false;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1) * cooldownDecreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    public void loadSkillConfig() {
        speedDuration = getConfig("speedDuration", 3.0, Double.class);
        slowDuration = getConfig("slowDuration", 3.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 2, Integer.class);
        speedStrength = getConfig("slownessStrength", 2, Integer.class);
    }

}
