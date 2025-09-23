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
import me.mykindos.betterpvp.core.client.gamer.Gamer;
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
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
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

import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class BullsCharge extends Skill implements Listener, InteractSkill, CooldownSkill, MovementSkill, DebuffSkill, BuffSkill {

    private final Map<Player, Long> running = new WeakHashMap<>();

    private final DisplayComponent durationActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable Player player = gamer.getPlayer();
                if (player == null) return null;

                int level = getLevel(gamer.getPlayer());
                if (level <= 0) return null;

                if (!running.containsKey(gamer.getPlayer())) return null;
                long timeLeftInMillis = running.get(player) - System.currentTimeMillis();

                // If true, ability has expired and `active` will be updated in the #onUpdate method within a couple ticks
                if (timeLeftInMillis <= 0) return null;
                double timeLeftInSeconds = timeLeftInMillis / 1000.0D;
                final String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(timeLeftInSeconds, 1);

                return getActionBarComponentForDuration("Charging", timeLeftWithOneDecimalPlace);
            }
    );

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
        final long speedDuration = (long) (getSpeedDuration(level) * 1000L);
        championsManager.getEffects().addEffect(player, EffectTypes.SPEED, getName(), speedStrength, speedDuration);
        running.put(player, System.currentTimeMillis() + speedDuration);
        championsManager.getCooldowns().removeCooldown(player, getName(), true);

        UtilSound.playSound(player.getWorld(), player, Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.OBSIDIAN);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, durationActionBar);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(durationActionBar);
    }

    @Override
    public boolean isPlayerCurrentlyUsingSkill(@NotNull Player player) {
        return running.containsKey(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player caster) {
            if (running.containsKey(caster)) {
                event.setKnockback(false);
            }
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (!(event.getDamager() instanceof Player caster)) {
            return;
        }

        if (running.containsKey(caster)) {
            final int level = getLevel(caster);
            doWhenBullsChargeExpires(caster, level);

            event.setKnockback(false);

            championsManager.getEffects().addEffect(event.getDamagee(), caster, EffectTypes.SLOWNESS, slownessStrength, (long) (getSlowDuration(level) * 1000L));
            championsManager.getEffects().removeEffect(caster, EffectTypes.SPEED, getName());

            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5F, 0.0F);
            caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5F, 0.5F);

            if (event.getDamagee() instanceof Player damaged) {
                UtilMessage.simpleMessage(damaged, getClassType().getName(), "<yellow>" + caster.getName() + "</yellow> hit you with <green>" + getName() + " " + level + "</green>.");
            }

            UtilMessage.simpleMessage(caster, getClassType().getName(), "You hit <yellow>" + event.getDamagee().getName() + "</yellow> with <green>" + getName() + " " + level + "</green>.");
            running.remove(caster);
        }
    }

    @EventHandler
    public void onKnockback(CustomEntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (running.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        running.entrySet().removeIf(entry -> {
            final @Nullable Player player = entry.getKey();  // not sure if nullable
            if (player == null || player.isDead() || !player.isOnline()) return true;

            // If ability ends naturally
            final boolean didPlayerTimeout = running.get(player) - System.currentTimeMillis() <= 0;
            final int level = getLevel(player);

            if (level <= 0 || didPlayerTimeout) {
                doWhenBullsChargeExpires(player, level);
                return true;
            }

            return false;  // do NOT remove player from map
        });
    }

    private void doWhenBullsChargeExpires(@NotNull Player player, int level) {
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);

        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), level));

    }

    @Override
    public double getCooldown(int level) {
        final double calculatedCooldown = cooldown - ((level - 1) * cooldownDecreasePerLevel);
        final double calculatedDuration = getSpeedDuration(level);

        return calculatedCooldown - calculatedDuration;
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
