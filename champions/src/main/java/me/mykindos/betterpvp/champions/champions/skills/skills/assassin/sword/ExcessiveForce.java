package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class ExcessiveForce extends Skill implements InteractSkill, CooldownSkill, Listener, OffensiveSkill {

    /**
     * Players using this ability mapped to their expiration time.
     * <p>
     * When the player uses this ability, we calculate the time, in milliseconds, in the future when the ability should
     * expire then store that value here.
     * <p>
     * See {@link #activate(Player, int)} for where that calculation is done.
     */
    private final WeakHashMap<Player, Long> active = new WeakHashMap<>();

    /**
     * An action bar component to show the remaining duration of excessive force.
     * <p>
     * Calculates the remaining time left based on the current time and the expiration time stored in {@link #active}.
     */
    private final DisplayComponent durationActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable Player player = gamer.getPlayer();
                if (player == null) return null;

                int level = getLevel(gamer.getPlayer());
                if (level <= 0) return null;

                if (!active.containsKey(gamer.getPlayer())) return null;
                long timeLeftInMillis = active.get(player) - System.currentTimeMillis();

                // If true, ability has expired and `active` will be updated in the #onUpdate method within a couple ticks
                if (timeLeftInMillis <= 0) return null;
                double timeLeftInSeconds = timeLeftInMillis / 1000.0D;
                final String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(timeLeftInSeconds, 1);

                // ex: Excessive Force: 2.6s
                return getActionBarComponentForDuration(timeLeftWithOneDecimalPlace);
            }
    );


    private double baseDuration;
    private double durationIncreasePerLevel;

    @Inject
    public ExcessiveForce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Excessive Force";
    }


    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "For the next " + getValueString(this::getDuration, level) + " seconds",
                "your attacks deal standard knockback to enemies",
                "",
                "Does not ignore anti-knockback abilities",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    /**
     * The duration of the excessive force ability for the given level.
     */
    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    // entrypoint
    @Override
    public void activate(Player player, int level) {

        // current time + duration in milliseconds = time in future when ability expires
        final long expirationTimeInMillis = System.currentTimeMillis() + (long) (getDuration(level) * 1000L);
        active.put(player, expirationTimeInMillis);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1.7f);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        gamer.getActionBar().add(900, durationActionBar);
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        gamer.getActionBar().remove(durationActionBar);
    }

    /**
     * This listener's purpose is to override the default no-knockback behavior of assassin's melee attacks.
     * <p>
     * Note: This might also affect other skills that set no-knockback, but currently there are none.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void setKnockback(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (active.containsKey(damager)) {
                event.setKnockback(true);
            }
        }
    }

    /**
     * Monitors active excessive force users and removes them when they expire.
     * Also removes players who no longer have the assassin role.
     */
    @UpdateEvent(delay = 100)
    public void onUpdate() {
        Iterator<Map.Entry<Player, Long>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Player, Long> next = it.next();
            Player player = next.getKey();

            if (!player.isOnline()) {
                it.remove();
                continue;
            }

            // No longer is assassin
            if (!championsManager.getRoles().hasRole(next.getKey(), Role.ASSASSIN)) {
                it.remove();
                continue;
            }

            // No longer has the skill
            if (getLevel(player) <= 0) {
                it.remove();
                continue;
            }

            // Remove ability if it expires
            if (next.getValue() - System.currentTimeMillis() <= 0) {
                it.remove();
                doWhenExcessiveForceExpires(player);
            }
        }
    }

    /**
     * Cleans up cooldowns and notifies the player when their excessive force duration ends naturally.
     * <p>
     * Non-natural expirations (like losing assassin role or logging off) are handled in {@link #onUpdate()}.
     */
    private void doWhenExcessiveForceExpires(@NotNull Player player) {
        int level = getLevel(player);
        if (level <= 0) return;  // #onUpdate already checks this, but just in case

        championsManager.getCooldowns().removeCooldown(player, getName(), true);
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);

        // Notify
        Component messageToSend = UtilMessage.deserialize("<green>%s %d</green> has ended.", getName(), level);
        UtilMessage.message(player, getClassType().getName(), messageToSend);
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return !active.containsKey(gamer.getPlayer());  // only show if the player is actively NOT using the skill
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public double getCooldown(int level) {
        final double calculatedCooldown = cooldown - ((level - 1) * cooldownDecreasePerLevel);
        final double calculatedDuration = getDuration(level);

        return calculatedCooldown - calculatedDuration;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        durationIncreasePerLevel = getConfig("durationPerLevel", 0.5, Double.class);
    }
}
