package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayComponent;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * A state skill is a skill that has a start time (contained in {@link #startStateTime}) and an end time given by
 * {@link #getStateDuration(int)}. Some state skills include Bulls Charge and Excessive Force.
 * <p>
 * States end either by reaching its natural end (just through the passage of time) or when some other action is
 * performed (like when a bulls charge user hits an enemy; thus, ending the state).
 */
public abstract class StateSkill extends Skill implements Listener, CooldownSkill, InteractSkill {

    /**
     * Contains all players who currently in an active state with this skill mapped to the duration of this ability's
     * state.
     */
    protected final Map<UUID, Double> activeState = new WeakHashMap<>();

    /**
     * Maps a player to their start time (in milliseconds); this player is removed from the map when the state ends
     * which is should coincide with them being removed from {@link #activeState}.
     */
    private final Map<UUID, Long> startStateTime = new WeakHashMap<>();

    /**
     * The action bar component that is updated in {@link #onUpdate()} and shown to the player while they are in
     * {@link #activeState}. This component will display the duration of this skill's state, in seconds.
     * <p>
     * Format:
     * <code>
     *     `name of skill`: `remaining duration of skill`s
     * </code>
     * <p>
     * Example:
     * <code>
     *     Excessive Force: 8.6s
     * </code>
     */
    private final DisplayComponent durationActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable Player player = gamer.getPlayer();
                if (player == null) return null;

                final int level = getLevel(gamer.getPlayer());
                if (level <= 0) return null;

                final @NotNull UUID uuid = gamer.getPlayer().getUniqueId();
                if (!activeState.containsKey(uuid)) return null;

                final long startTime = startStateTime.get(uuid);
                final long stateDurationInMillis = (long) (activeState.get(uuid) * 1000L);

                // startTime + duration - currentTime
                final long timeLeftInMillis = (startTime + stateDurationInMillis) - System.currentTimeMillis();

                // If true, ability has expired and `activeState` will be updated in the #onUpdate method within a couple ticks
                if (timeLeftInMillis <= 0) return null;
                final double timeLeftInSeconds = timeLeftInMillis / 1000.0D;
                final String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(timeLeftInSeconds, 1, true);

                return getActionBarComponentForDuration(getActionBarLabel(), timeLeftWithOneDecimalPlace);
            }
    );

    public StateSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    /**
     * The message/label displayed before the duration of the cooldown.
     * <p>
     * Format - `label`: 8.6s
     * <p>
     * Example - Charging: 0.9s
     */
    abstract protected @NotNull String getActionBarLabel();

    /**
     * The duration of the state (in seconds). This is needed to calculate the cooldown of the ability.
     */
    abstract protected double getStateDuration(int level);

    /**
     * Adds the player to {@link #activeState} and tracks the current time when this ability started.
     */
    @Override
    public void activate(Player player, int level) {
        final double stateDurationInSeconds = getStateDuration(level);
        final @NotNull UUID uuid = player.getUniqueId();

        activeState.put(uuid, stateDurationInSeconds);
        startStateTime.put(uuid, System.currentTimeMillis());
    }

    /**
     * Removes the player from {@link #activeState}, starts the cooldown for this skill, and sends a message to the
     * player signifying that the ability has ended.
     */
    protected void doWhenStateEnds(@NotNull UUID uuid) {
        activeState.remove(uuid);
        startStateTime.remove(uuid);

        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // If the player doesnt have the skill, we still want to apply the cooldown
        final int level = Math.max(1, getLevel(player));

        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);

        // If the player doesnt have the skill, don't show the message
        if (getLevel(player) <= 0) return;
        UtilMessage.message(player, getClassType().getName(), UtilMessage.deserialize("<green>%s %s</green> has ended.", getName(), level));
    }

    /**
     * Cleanup state when player dies.
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        doWhenStateEnds(event.getPlayer().getUniqueId());
    }

    /**
     * Cleanup state when player quits.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        doWhenStateEnds(event.getPlayer().getUniqueId());
    }

    /**
     * Verify player's data is still valid; check if the player is dead or offline, or if the skill has timed out (i.e.
     * the state has expired).
     */
    @UpdateEvent(delay = 100)
    public void onUpdate() {
        final Iterator<Map.Entry<UUID, Double>> iterator = activeState.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Double> entry = iterator.next();

            final @NotNull UUID uuid = entry.getKey();
            final double stateDuration = entry.getValue();

            // we dont care about cds or messages if the player is dead or gone
            final @Nullable Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || player.isDead() || !player.isOnline()) {
                iterator.remove();
                startStateTime.remove(uuid);
                continue;
            }

            // If ability ends naturally
            final long stateDurationInMillis = (long) (stateDuration * 1000L);
            final boolean hasTimedOut = UtilTime.elapsed(startStateTime.get(uuid), stateDurationInMillis);

            if (getLevel(player) <= 0 || hasTimedOut || !championsManager.getRoles().hasRole(player, getClassType())) {
                doWhenStateEnds(player.getUniqueId());
            }
        }
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
    public boolean canUse(Player player) {
        return !activeState.containsKey(player.getUniqueId());
    }

    @Override
    public double getCooldown(int level) {
        final double calculatedCooldown = cooldown - ((level - 1) * cooldownDecreasePerLevel);
        final double calculatedDuration = getStateDuration(level);

        return calculatedCooldown - calculatedDuration;
    }
}
