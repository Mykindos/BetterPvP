package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
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

public abstract class StateSkill extends Skill implements Listener, CooldownSkill {

    /**
     * Contains all players who currently in an active state with this skill mapped to the duration of this ability's
     * state.
     */
    protected final Map<UUID, Long> activeState = new WeakHashMap<>();

    private final DisplayComponent durationActionBar = new PermanentComponent(
            gamer -> {
                final @Nullable Player player = gamer.getPlayer();
                if (player == null) return null;

                final int level = getLevel(gamer.getPlayer());
                if (level <= 0) return null;

                final @NotNull UUID uuid = gamer.getPlayer().getUniqueId();
                if (!activeState.containsKey(uuid)) return null;

                final long timeLeftInMillis = activeState.get(uuid) - System.currentTimeMillis();

                // If true, ability has expired and `activeState` will be updated in the #onUpdate method within a couple ticks
                if (timeLeftInMillis <= 0) return null;
                final double timeLeftInSeconds = timeLeftInMillis / 1000.0D;
                final String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(timeLeftInSeconds, 1);

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
     * The duration of the state. This is needed to calculate the cooldown of the ability.
     */
    abstract protected double getStateDuration(int level);

    /**
     * Removes the player from {@link #activeState}, starts the cooldown for this skill, and sends a message to the
     * player signifying that the ability has ended.
     */
    protected void doWhenStateExpires(@NotNull UUID uuid) {
        activeState.remove(uuid);

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

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        doWhenStateExpires(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        doWhenStateExpires(event.getPlayer().getUniqueId());
    }

    @UpdateEvent(delay = 100)
    public void onUpdate() {
        final Iterator<Map.Entry<UUID, Long>> iterator = activeState.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<UUID, Long> entry = iterator.next();
            final @Nullable Player player = Bukkit.getPlayer(entry.getKey());
            final long expirationTime = entry.getValue();

            // we dont care about cds or messages if the player is dead or gone
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
