package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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

                // If true, ability has expired and `active` will be updated in the #onUpdate method within a couple ticks
                if (timeLeftInMillis <= 0) return null;
                final double timeLeftInSeconds = timeLeftInMillis / 1000.0D;
                final String timeLeftWithOneDecimalPlace = UtilFormat.formatNumber(timeLeftInSeconds, 1);

                return getActionBarComponentForDuration(getActionBarLabel(), timeLeftWithOneDecimalPlace);
            }
    );

    public StateSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
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

    /**
     * The message/label displayed before the duration of the cooldown.
     * <p>
     * Format - `label`: 8.6s
     * <p>
     * Example - Charging: 0.9s
     */
    abstract protected @NotNull String getActionBarLabel();

    /**
     * Removes the player from {@link #activeState} and starts the cooldown for this skill.
     */
    protected void doWhenStateExpires(@NotNull UUID uuid) {
        activeState.remove(uuid);

        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        // If the player doesnt have the skill, we still want to apply the cooldown
        int level = Math.max(1, getLevel(player));

        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                showCooldownFinished(),
                true,
                isCancellable(),
                this::shouldDisplayActionBar);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        doWhenStateExpires(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        doWhenStateExpires(event.getPlayer().getUniqueId());
    }
}
