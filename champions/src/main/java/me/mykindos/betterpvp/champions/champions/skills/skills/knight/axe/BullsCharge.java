package me.mykindos.betterpvp.champions.champions.skills.skills.knight.axe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.MovementSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.StateSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Singleton
@BPvPListener
public class BullsCharge extends StateSkill implements Listener, InteractSkill, CooldownSkill, MovementSkill, DebuffSkill, BuffSkill {

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
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with an Axe to activate.",
                "",
                "Begin charging, gaining <effect>Speed " + UtilFormat.getRomanNumeral(speedStrength) + "</effect>",
                "for " + getValueString(this::getSpeedDuration, level) + " seconds.",
                "",
                "Hitting an enemy <effect>Slows</effect> them for",
                getValueString(this::getSlowDuration, level) + " seconds and ends your",
                "charging.",
                "",
                "While charging, you take no knockback.",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    /**
     * @return the duration of the speed effect granted to the caster.
     */
    public double getSpeedDuration(int level) {
        return speedDuration + (level - 1) * speedDurationIncreasePerLevel;
    }

    /**
     * @return the duration of the slowness effect applied to the first hit enemy
     */
    public double getSlowDuration(int level) {
        return slowDuration + (level - 1) * slowDurationIncreasePerLevel;
    }

    // entrypt
    @Override
    public void activate(Player player, int level) {

        final long speedDurationMillis = (long) (getSpeedDuration(level) * 1000L);
        activeState.put(player.getUniqueId(), System.currentTimeMillis() + speedDurationMillis);

        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, speedDurationMillis);

        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.OBSIDIAN);
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player caster) {
            if (activeState.containsKey(caster.getUniqueId())) {
                event.setKnockback(false);
            }
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (!(event.getDamager() instanceof Player caster)) {
            return;
        }

        if (activeState.containsKey(caster.getUniqueId())) {
            final int level = getLevel(caster);
            doWhenStateExpires(caster.getUniqueId());

            event.setKnockback(false);

            championsManager.getEffects().addEffect(event.getDamagee(), caster, EffectTypes.SLOWNESS, slownessStrength, (long) (getSlowDuration(level) * 1000L));
            championsManager.getEffects().removeEffect(caster, EffectTypes.SPEED, getName());

            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.0F);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5F, 0.5F);

            if (event.getDamagee() instanceof Player damaged) {
                UtilMessage.simpleMessage(damaged, getClassType().getName(), "<yellow>" + caster.getName() + "</yellow> hit you with <green>" + getName() + " " + level + "</green>.");
            }

            UtilMessage.simpleMessage(caster, getClassType().getName(), "You hit <yellow>" + event.getDamagee().getName() + "</yellow> with <green>" + getName() + " " + level + "</green>.");
        }
    }

    @EventHandler
    public void onKnockback(CustomEntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (activeState.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        final Iterator<Map.Entry<UUID, Long>> iterator = activeState.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            final @Nullable Player player = Bukkit.getPlayer(entry.getKey());
            final long expirationTime = entry.getValue();

            if (player == null || player.isDead() || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            // If ability ends naturally
            final boolean didPlayerTimeout = expirationTime - System.currentTimeMillis() <= 0;
            final int level = getLevel(player);

            if (level <= 0 || didPlayerTimeout) {
                doWhenStateExpires(player.getUniqueId());
            }

            // play some particles
        }
    }

    @Override
    protected void doWhenStateExpires(@NotNull UUID uuid) {
        super.doWhenStateExpires(uuid);

        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        final int level = getLevel(player);
        if (level <= 0) return;  // dont show a msg if they dont have the skill anymore

        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), level));
    }

    @Override
    public double getCooldown(int level) {
        final double calculatedCooldown = cooldown - ((level - 1) * cooldownDecreasePerLevel);
        final double calculatedDuration = getSpeedDuration(level);

        return calculatedCooldown - calculatedDuration;
    }

    @Override
    protected @NotNull String getActionBarLabel() {
        return "Charging";
    }

    @Override
    public String getName() {
        return "Bulls Charge";
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
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
        speedDuration = getConfig("speedDuration", 5.0, Double.class);
        speedDurationIncreasePerLevel = getConfig("speedDurationIncreasePerLevel", 0.0, Double.class);
        speedStrength = getConfig("speedStrength", 3, Integer.class);

        slowDuration = getConfig("slowDuration", 3.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);
        slownessStrength = getConfig("slownessStrength", 3, Integer.class);
    }

}
