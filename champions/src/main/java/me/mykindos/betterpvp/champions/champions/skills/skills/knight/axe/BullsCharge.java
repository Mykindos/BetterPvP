package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.UUID;

@Singleton
@BPvPListener
public class BullsCharge extends Skill implements Listener, InteractSkill, CooldownSkill, MovementSkill, DebuffSkill, BuffSkill {

    private final HashMap<UUID, Long> running = new HashMap<>();

    private double speedDuration;
    private double speedDurationIncreasePerLevel;

    private double slowDuration;
    private double slowDurationIncreasePerLevel;

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
                "Enter a rage, gaining <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect> for " + getValueString(this::getSpeedDuration, level) + " seconds",
                "and giving <effect>Slowness " + UtilFormat.getRomanNumeral(slownessStrength) + "</effect> to anything you hit for " + getValueString(this::getSlowDuration, level) + " seconds",
                "",
                "While charging, you take no knockback",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)

        };
    }

    public double getSpeedDuration(int level) {
        return speedDuration + (level - 1) * speedDurationIncreasePerLevel;
    }

    public double getSlowDuration(int level) {
        return slowDuration + (level - 1) * slowDurationIncreasePerLevel;
    }

    @Override
    public void activate(Player player, int level) {
        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, (long) (getSpeedDuration(level) * 1000L));
        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.OBSIDIAN);
        running.put(player.getUniqueId(), System.currentTimeMillis() + (long)(getSpeedDuration(level) * 1000));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamagee() instanceof Player player) {
            if (running.containsKey(player.getUniqueId())) {
                event.setKnockback(false);
            }
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (event.getDamager() instanceof Player damager) {

            final LivingEntity damagee = event.getDamagee();

            if (running.containsKey(damager.getUniqueId())) {
                if (expire(damager.getUniqueId())) {
                    running.remove(damager.getUniqueId());
                    return;
                }
                int level = getLevel(damager);
                event.setKnockback(false);

                championsManager.getEffects().addEffect(damagee, damager, EffectTypes.SLOWNESS, slownessStrength, (long) (getSlowDuration(level) * 1000L));
                championsManager.getEffects().removeEffect(damager, EffectTypes.SPEED, getName());

                damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.0F);
                damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5F, 0.5F);

                if (event.getDamagee() instanceof Player damaged) {
                    UtilMessage.simpleMessage(damaged, getClassType().getName(), "<yellow>" + damager.getName() + "</yellow> hit you with <green>" + getName() + " " + level + "</green>.");
                }

                UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <yellow>" + event.getDamagee().getName() + "</yellow> with <green>" + getName() + " " + level + "</green>.");
                running.remove(damager.getUniqueId());
            }
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        running.entrySet().removeIf(entry -> expire(entry.getKey()));
    }

    private boolean expire(UUID uiid) {
        if (running.get(uiid) - System.currentTimeMillis() <= 0) {
            Player player = Bukkit.getPlayer(uiid);
            if (player == null) {
                return true;
            }

            int level = getLevel(player);
            UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), level));
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

    @Override
    public void loadSkillConfig() {
        speedDuration = getConfig("speedDuration", 3.0, Double.class);
        speedDurationIncreasePerLevel = getConfig("speedDurationIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 3, Integer.class);

        slowDuration = getConfig("slowDuration", 3.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
    }

}
